package com.example.querysence.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.example.querysence.model.ColumnDefinition;
import com.example.querysence.model.IndexDefinition;
import com.example.querysence.model.TableDefinition;
import com.example.querysence.model.dto.IndexSuggestionResponse;
import com.example.querysence.parser.ParsedQuery;
import com.example.querysence.repository.SchemaDefinitionRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndexAdvisorService {

    private final SchemaDefinitionRepository schemaRepository;

    private static final int MAX_COMPOSITE_WIDTH = 4;
    private static final Set<String> RANGE_OPERATORS = Set.of(">", "<", ">=", "<=", "BETWEEN", "LIKE");

    public List<IndexSuggestionResponse> suggestIndexes(ParsedQuery parsedQuery, Long schemaId) {
        Map<String, List<List<String>>> existingIndexColumnLists = new HashMap<>();
        Map<String, Long> tableRowCounts = new HashMap<>();
        Map<String, Map<String, Double>> distinctCountsByTable = new HashMap<>();

        // Load existing indexes from schema if provided
        if (schemaId != null) {
            schemaRepository.findByIdWithFullDetails(schemaId).ifPresent(schema -> {
                for (TableDefinition table : schema.getTables()) {
                    String tableName = table.getTableName().toLowerCase();
                    tableRowCounts.put(tableName, table.getEstimatedRows());
                    for (ColumnDefinition column : table.getColumns()) {
                        distinctCountsByTable
                                .computeIfAbsent(tableName, k -> new HashMap<>())
                                .put(column.getColumnName().toLowerCase(), column.getDistinctCount());
                    }

                    List<List<String>> indexlists = new ArrayList<>();
                    for (IndexDefinition index : table.getIndexes()) {
                        indexlists.add(index.getColumns().stream()
                                .map(String::toLowerCase)
                                .collect(Collectors.toList()));
                    }
                    existingIndexColumnLists.put(tableName, indexlists);
                }
            });
        }

        List<IndexSuggestionResponse> suggestions = new ArrayList<>();

        Map<String, Set<String>> equalityColumnsByTable = new HashMap<>();
        Map<String, Set<String>> rangeColumnsByTable = new HashMap<>();
        // Analyze WHERE clause columns
        for (ParsedQuery.WhereCondition condition : parsedQuery.getWhereConditions()) {
            String table = condition.getTable() == null ? "" : condition.getTable().toLowerCase();
            String column = condition.getColumn().toLowerCase();

            if (table.isEmpty() && parsedQuery.getTables().size() == 1) {
                table = parsedQuery.getTables().get(0).toLowerCase();
            }
            if (table.isEmpty()) {
                log.warn("Could not determine table for WHERE condition: {}", condition);
                continue;
            }
            boolean range = condition.getOperator() != null
                    && RANGE_OPERATORS.contains(condition.getOperator().toUpperCase());

            if (range) {
                rangeColumnsByTable.computeIfAbsent(table, k -> new LinkedHashSet<>()).add(column);
            } else {
                equalityColumnsByTable.computeIfAbsent(table, k -> new LinkedHashSet<>()).add(column);
            }

        }

        // Analyze JOIN columns
        Map<String, Set<String>> joinColumnsByTable = new HashMap<>();
        for (ParsedQuery.JoinInfo join : parsedQuery.getJoins()) {
            String table = join.getTable() == null ? "" : join.getTable().toLowerCase();
            if (table.isEmpty()) {
                log.warn("Could not determine table for JOIN: {}", join);
                continue;
            }
            for (String col : join.getJoinColumns()) {
                joinColumnsByTable.computeIfAbsent(table, k -> new LinkedHashSet<>()).add(col.toLowerCase());
            }
        }

        // Analyze ORDER BY columns
        Map<String, Set<String>> orderByColumnsByTable = new HashMap<>();
        for (String orderCol : parsedQuery.getOrderByColumns()) {
            // Try to find the table for this column
            for (String table : parsedQuery.getTables()) {
                orderByColumnsByTable.computeIfAbsent(table.toLowerCase(), k -> new LinkedHashSet<>())
                        .add(orderCol.toLowerCase());
            }
        }

        // Analyze GROUP BY columns
        Map<String, Set<String>> groupByColumnsByTable = new HashMap<>();
        for (String groupCol : parsedQuery.getGroupByColumns()) {
            for (String table : parsedQuery.getTables()) {
                groupByColumnsByTable.computeIfAbsent(table.toLowerCase(), k -> new LinkedHashSet<>())
                        .add(groupCol.toLowerCase());
            }
        }

        // Generate suggestions for each table
        Set<String> allTables = new HashSet<>();
        allTables.addAll(equalityColumnsByTable.keySet());
        allTables.addAll(rangeColumnsByTable.keySet());
        allTables.addAll(joinColumnsByTable.keySet());
        allTables.addAll(orderByColumnsByTable.keySet());
        allTables.addAll(groupByColumnsByTable.keySet());

        for (String table : allTables) {
            List<List<String>> existing = existingIndexColumnLists.getOrDefault(table, Collections.emptyList());
            Long rowCount = tableRowCounts.getOrDefault(table, 0L);

            Map<String, Double> distinctCounts = distinctCountsByTable.getOrDefault(table, Collections.emptyMap());
            Set<String> joinCols = joinColumnsByTable.getOrDefault(table, Collections.emptySet());
            Set<String> equalityCols = equalityColumnsByTable.getOrDefault(table, Collections.emptySet());
            Set<String> rangeCols = rangeColumnsByTable.getOrDefault(table, Collections.emptySet());
            Set<String> orderCols = orderByColumnsByTable.getOrDefault(table, Collections.emptySet());
            Set<String> groupCols = groupByColumnsByTable.getOrDefault(table, Collections.emptySet());

            List<String> composite = buildCompositeColumns(
                    joinCols, equalityCols, rangeCols, orderCols, existing, distinctCounts, rowCount);

            if (!composite.isEmpty()) {
                // boolean hasRange = composite.stream().anyMatch(rangeCols::contains);
                String impact = calculateImpact(
                        rowCount,
                        distinctCounts.get(composite.get(0)),
                        true,
                        !joinCols.isEmpty());
                String type = composite.size() > 1 ? "COMPOSITE" : "SINGLE";
                String reason = describeComposite(composite, joinCols, equalityCols, rangeCols, orderCols);
                suggestions.add(createSuggestion(table, composite, type, impact, reason));
            }
            List<String> uncoveredGroupCols = groupCols.stream()
                    .filter(c -> !composite.contains(c))
                    .filter(c -> !isLeadingColumnOfAnyIndex(c, existing))
                    .sorted(bySelectivityDesc(distinctCounts, rowCount))
                    .limit(MAX_COMPOSITE_WIDTH)
                    .collect(Collectors.toList());

            if (!uncoveredGroupCols.isEmpty()) {
                String type = uncoveredGroupCols.size() > 1 ? "COMPOSITE" : "SINGLE";
                suggestions.add(createSuggestion(table, uncoveredGroupCols, type, "LOW", "Used in GROUP BY"));
            }
        }

        return suggestions;
    }

    private boolean isLeadingColumnOfAnyIndex(String column, List<List<String>> indexLists) {
        return indexLists.stream()
                .anyMatch(cols -> !cols.isEmpty() && cols.get(0).equals(column));
    }

    private boolean isCompositeConveredByExistingIndex(List<String> candidate, List<List<String>> indexLists) {
        return indexLists.stream()
                .anyMatch(existing -> existing.size() >= candidate.size() &&
                        existing.subList(0, candidate.size()).equals(candidate));
    }

    private List<String> buildCompositeColumns(
            Set<String> joinCols,
            Set<String> equalityCols,
            Set<String> rangeCols,
            Set<String> orderCols,
            List<List<String>> indexLists, Map<String, Double> distinctCounts,
            Long rowCount) {

        LinkedHashSet<String> equalityLike = new LinkedHashSet<>();
        equalityLike.addAll(joinCols);
        equalityLike.addAll(equalityCols);
        equalityLike.removeIf(col -> isLeadingColumnOfAnyIndex(col, indexLists));

        List<String> sortedColumns = equalityLike.stream()
                .sorted(bySelectivityDesc(distinctCounts, rowCount))
                .collect(Collectors.toList());

        List<String> ordered;

        if (sortedColumns.size() > MAX_COMPOSITE_WIDTH) {
            ordered = new ArrayList<>(
                    sortedColumns.subList(0, MAX_COMPOSITE_WIDTH));
        } else {
            ordered = new ArrayList<>(sortedColumns);
        }
        if (ordered.size() < MAX_COMPOSITE_WIDTH) {
            rangeCols.stream()
                    .filter(c -> !isLeadingColumnOfAnyIndex(c, indexLists) && !ordered.contains(c))
                    .findFirst()
                    .ifPresent(ordered::add);
        }
        for (String col : orderCols) {
            if (ordered.size() >= MAX_COMPOSITE_WIDTH) {
                break;
            }
            if (!isLeadingColumnOfAnyIndex(col, indexLists) && !ordered.contains(col)) {
                ordered.add(col);
            }
        }

        if (ordered.isEmpty() || isCompositeConveredByExistingIndex(ordered, indexLists)) {
            return Collections.emptyList();
        }

        return ordered;
    }

    private Comparator<String> bySelectivityDesc(Map<String, Double> distinctCounts, Long rowCount) {
        return Comparator.comparingDouble((String col) -> {
            Double distinct = distinctCounts.get(col);
            if (distinct == null || rowCount == null || rowCount <= 0) {
                return 0.0;
            }
            return distinct / rowCount;
        }).reversed();
    }

    private String describeComposite(List<String> composite, Set<String> joinCols, Set<String> equalityCols,
            Set<String> rangeCols, Set<String> orderCols) {
        List<String> reasons = new ArrayList<>();
        if (composite.stream().anyMatch(joinCols::contains))
            reasons.add("JOIN");
        if (composite.stream().anyMatch(equalityCols::contains))
            reasons.add("WHERE equality");
        if (composite.stream().anyMatch(rangeCols::contains))
            reasons.add("WHERE range");
        if (composite.stream().anyMatch(orderCols::contains))
            reasons.add("ORDER BY");
        return "Covers " + String.join(" + ", reasons);
    }

    private String calculateImpact(Long rowCount, Double distinctCount, boolean inWhere, boolean inJoin) {
        double selectivity = (distinctCount != null && rowCount != null && rowCount > 0)
                ? distinctCount / rowCount
                : 1.0;

        if (selectivity < 0.01 && !inJoin) {
            return "LOW";
        }

        if (inJoin && inWhere) {
            return "HIGH";
        }

        if (inJoin || (inWhere && rowCount != null && rowCount > 10000)) {
            return "HIGH";
        }

        if (inWhere && rowCount != null && rowCount > 1000) {
            return "MEDIUM";
        }

        return "LOW";
    }

    private IndexSuggestionResponse createSuggestion(String table, List<String> columns,
            String type, String impact, String reasoning) {
        String indexName = "idx_" + table + "_" + String.join("_", columns);
        String createStatement = generateCreateStatement(table, columns, indexName);

        return IndexSuggestionResponse.builder()
                .tableName(table)
                .columns(columns)
                .indexName(indexName)
                .suggestionType(type)
                .impactScore(impact)
                .reasoning(reasoning)
                .createStatement(createStatement)
                .build();
    }

    private String generateCreateStatement(String table, List<String> columns, String indexName) {
        return String.format("CREATE INDEX %s ON %s (%s);",
                indexName, table, String.join(", ", columns));
    }
}

package com.example.querysence.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

    public List<IndexSuggestionResponse> suggestIndexes(ParsedQuery parsedQuery, Long schemaId) {
        Map<String, Set<String>> existingIndexes = new HashMap<>();
        Map<String, Long> tableRowCounts = new HashMap<>();

        // Load existing indexes from schema if provided
        if (schemaId != null) {
            schemaRepository.findByIdWithFullDetails(schemaId).ifPresent(schema -> {
                for (TableDefinition table : schema.getTables()) {
                    String tableName = table.getTableName().toLowerCase();
                    tableRowCounts.put(tableName, table.getEstimatedRows());
                    
                    Set<String> indexedColumns = new HashSet<>();
                    for (IndexDefinition index : table.getIndexes()) {
                        indexedColumns.addAll(index.getColumns().stream()
                                .map(String::toLowerCase)
                                .collect(Collectors.toSet()));
                    }
                    existingIndexes.put(tableName, indexedColumns);
                }
            });
        }

        List<IndexSuggestionResponse> suggestions = new ArrayList<>();

        // Analyze WHERE clause columns
        Map<String, Set<String>> whereColumnsByTable = new HashMap<>();
        for (ParsedQuery.WhereCondition condition : parsedQuery.getWhereConditions()) {
            String table = condition.getTable().toLowerCase();
            String column = condition.getColumn().toLowerCase();
            
            if (table.isEmpty() && parsedQuery.getTables().size() == 1) {
                table = parsedQuery.getTables().get(0).toLowerCase();
            }
            
            if (!table.isEmpty()) {
                whereColumnsByTable.computeIfAbsent(table, k -> new LinkedHashSet<>()).add(column);
            }
        }

        // Analyze JOIN columns
        Map<String, Set<String>> joinColumnsByTable = new HashMap<>();
        for (ParsedQuery.JoinInfo join : parsedQuery.getJoins()) {
            String table = join.getTable().toLowerCase();
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
        allTables.addAll(whereColumnsByTable.keySet());
        allTables.addAll(joinColumnsByTable.keySet());
        allTables.addAll(orderByColumnsByTable.keySet());
        allTables.addAll(groupByColumnsByTable.keySet());

        for (String table : allTables) {
            Set<String> existing = existingIndexes.getOrDefault(table, Collections.emptySet());
            Long rowCount = tableRowCounts.getOrDefault(table, 0L);

            // Check for composite index opportunity (WHERE + JOIN)
            Set<String> whereCols = whereColumnsByTable.getOrDefault(table, Collections.emptySet());
            Set<String> joinCols = joinColumnsByTable.getOrDefault(table, Collections.emptySet());
            
            Set<String> combinedCols = new LinkedHashSet<>();
            // Order: JOIN columns first (more selective for joins), then WHERE columns
            combinedCols.addAll(joinCols);
            combinedCols.addAll(whereCols);
            
            // Remove already indexed columns
            combinedCols.removeAll(existing);
            
            if (combinedCols.size() >= 2) {
                String impact = calculateImpact(rowCount, true, joinCols.size() > 0);
                suggestions.add(createSuggestion(
                        table, 
                        new ArrayList<>(combinedCols), 
                        "COMPOSITE", 
                        impact,
                        "Composite index for WHERE and JOIN conditions"
                ));
            } else if (combinedCols.size() == 1) {
                String column = combinedCols.iterator().next();
                String impact = calculateImpact(rowCount, whereCols.contains(column), joinCols.contains(column));
                String reason = joinCols.contains(column) ? "Used in JOIN condition" : "Used in WHERE clause";
                suggestions.add(createSuggestion(
                        table, 
                        List.of(column), 
                        "SINGLE", 
                        impact, 
                        reason
                ));
            }

            // ORDER BY index suggestion
            Set<String> orderCols = orderByColumnsByTable.getOrDefault(table, Collections.emptySet());
            orderCols.removeAll(existing);
            for (String col : orderCols) {
                if (!combinedCols.contains(col)) {
                    suggestions.add(createSuggestion(
                            table, 
                            List.of(col), 
                            "SINGLE", 
                            "MEDIUM",
                            "Used in ORDER BY - improves sorting performance"
                    ));
                }
            }

            // GROUP BY index suggestion (lower priority)
            Set<String> groupCols = groupByColumnsByTable.getOrDefault(table, Collections.emptySet());
            groupCols.removeAll(existing);
            for (String col : groupCols) {
                if (!combinedCols.contains(col) && !orderCols.contains(col)) {
                    suggestions.add(createSuggestion(
                            table, 
                            List.of(col), 
                            "SINGLE", 
                            "LOW",
                            "Used in GROUP BY"
                    ));
                }
            }
        }

        return suggestions;
    }

    private String calculateImpact(Long rowCount, boolean inWhere, boolean inJoin) {
        if (inJoin && inWhere) {
            return "HIGH";
        }
        if (inJoin || (inWhere && rowCount > 10000)) {
            return "HIGH";
        }
        if (inWhere && rowCount > 1000) {
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

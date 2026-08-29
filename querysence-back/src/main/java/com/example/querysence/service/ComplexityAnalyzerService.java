package com.example.querysence.service;

import java.util.ArrayList;
import java.util.Objects;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.querysence.model.IndexDefinition;
import com.example.querysence.model.TableDefinition;
import com.example.querysence.model.dto.ComplexityReport;
import com.example.querysence.parser.ParsedQuery;
import com.example.querysence.repository.SchemaDefinitionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComplexityAnalyzerService {

    private final SchemaDefinitionRepository schemaRepository;

    private static final int BASE_SCORE = 10;
    private static final int EXTRA_TABLE_POINTS = 3;
    private static final int JOIN_POINTS = 8;
    private static final int SUBQUERY_POINTS = 12;
    private static final int AGGREGATE_POINTS = 3;
    private static final int GROUP_BY_POINTS = 3;
    private static final int ORDER_BY_POINTS = 2;
    private static final int DISTINCT_POINTS = 3;
    private static final int GROUP_BY_HAVING_POINTS = 5;
    private static final int WHERE_CONDITION_POINTS = 1;
    private static final int EXTRA_WHERE_CONDITION_POINTS = 1;
    private static final int WHERE_THRESHOLD = 5;

    private static final long LARGE_TABLE_ROWS = 100_000L;
    private static final long HUGE_TABLE_ROWS = 1_000_000L;
    private static final int LARGE_TABLE_POINTS = 5;
    private static final int HUGE_TABLE_POINTS = 12;
    private static final int UNINDEXED_FILTER_ON_LARGE_TABLE_POINTS = 10;

    public ComplexityReport analyze(ParsedQuery parsedQuery) {
        return analyze(parsedQuery, null);
    }

    public ComplexityReport analyze(ParsedQuery parsedQuery, Long schemaId) {
        List<ComplexityReport.Factor> factors = new ArrayList<>();
        int score = BASE_SCORE;

        int tableCount = parsedQuery.getTables().size();
        if (tableCount > 1) {
            int tableScore = (tableCount - 1) * EXTRA_TABLE_POINTS;
            score += tableScore;
            factors.add(ComplexityReport.Factor.builder()
                    .name("Tables")
                    .count(tableCount)
                    .points(tableScore)
                    .description(tableCount + " table(s) involved")
                    .build());
        }

        // Count joins
        int joinCount = parsedQuery.getJoins().size();
        if (joinCount > 0) {
            int joinScore = joinCount * JOIN_POINTS;
            score += joinScore;
            factors.add(ComplexityReport.Factor.builder()
                    .name("Joins")
                    .count(joinCount)
                    .points(joinScore)
                    .description(joinCount + " JOIN operation(s) detected")
                    .build());
        }

        // Count subqueries with depth multiplier
        int subqueryScore = calculateSubqueryScore(parsedQuery);
        if (subqueryScore > 0) {
            int subqueryCount = countTotalSubqueries(parsedQuery);
            score += subqueryScore;
            factors.add(ComplexityReport.Factor.builder()
                    .name("Subqueries")
                    .count(subqueryCount)
                    .points(subqueryScore)
                    .description("Nested subqueries increase complexity exponentially")
                    .build());
        }

        // Count aggregate functions
        int aggregateCount = parsedQuery.getAggregateFunctions().size();
        if (aggregateCount > 0) {
            int aggScore = aggregateCount * AGGREGATE_POINTS;
            score += aggScore;
            factors.add(ComplexityReport.Factor.builder()
                    .name("Aggregates")
                    .count(aggregateCount)
                    .points(aggScore)
                    .description("Aggregate functions: " + String.join(", ", parsedQuery.getAggregateFunctions()))
                    .build());
        }

        // Check DISTINCT
        if (parsedQuery.isHasDistinct()) {
            score += DISTINCT_POINTS;
            factors.add(ComplexityReport.Factor.builder()
                    .name("DISTINCT")
                    .count(1)
                    .points(DISTINCT_POINTS)
                    .description("DISTINCT requires sorting/hashing for deduplication")
                    .build());
        }

        // Check GROUP BY with HAVING
        if (parsedQuery.isHasHaving() && !parsedQuery.getGroupByColumns().isEmpty()) {
            score += GROUP_BY_HAVING_POINTS;
            factors.add(ComplexityReport.Factor.builder()
                    .name("GROUP BY + HAVING")
                    .count(1)
                    .points(GROUP_BY_HAVING_POINTS)
                    .description("HAVING clause filters grouped results")
                    .build());
        } else if (!parsedQuery.getGroupByColumns().isEmpty()) {
            score += GROUP_BY_POINTS;
            factors.add(ComplexityReport.Factor.builder()
                    .name("GROUP BY")
                    .count(parsedQuery.getGroupByColumns().size())
                    .points(GROUP_BY_POINTS)
                    .description("Grouping increases sort/hash work")
                    .build());
        }

        if (!parsedQuery.getOrderByColumns().isEmpty()) {
            score += ORDER_BY_POINTS;
            factors.add(ComplexityReport.Factor.builder()
                    .name("ORDER BY")
                    .count(parsedQuery.getOrderByColumns().size())
                    .points(ORDER_BY_POINTS)
                    .description("Ordering adds sorting cost")
                    .build());
        }

        // WHERE conditions baseline + extra cost beyond threshold
        int whereCount = parsedQuery.getWhereConditions().size();
        if (whereCount > 0) {
            int baselineWhereScore = Math.min(whereCount, WHERE_THRESHOLD) * WHERE_CONDITION_POINTS;
            score += baselineWhereScore;
            factors.add(ComplexityReport.Factor.builder()
                    .name("WHERE Conditions")
                    .count(whereCount)
                    .points(baselineWhereScore)
                    .description(whereCount + " filtering condition(s)")
                    .build());
        }
        if (whereCount > WHERE_THRESHOLD) {
            int extraConditions = whereCount - WHERE_THRESHOLD;
            int extraScore = extraConditions * EXTRA_WHERE_CONDITION_POINTS;
            score += extraScore;
            factors.add(ComplexityReport.Factor.builder()
                    .name("WHERE Overhead")
                    .count(extraConditions)
                    .points(extraScore)
                    .description(whereCount + " conditions (>" + WHERE_THRESHOLD + " threshold)")
                    .build());
        }

        // Table volume: how big are the tables actually involved? Only meaningful if a
        // schema was supplied - otherwise we only know table *names* from the SQL text,
        // not their size, so this section (and its score contribution) is skipped.
        List<String> tableSizeWarnings = new ArrayList<>();
        if (schemaId != null) {
            Map<String, Long> tableRowCounts = new HashMap<>();
            Map<String, Set<String>> existingIndexes = new HashMap<>();
            schemaRepository.findByIdWithFullDetails(schemaId).ifPresent(schema -> {
                for (TableDefinition table : schema.getTables()) {
                    String tableName = table.getTableName().toLowerCase(Locale.ROOT);
                    tableRowCounts.put(tableName, Objects.requireNonNullElse(table.getEstimatedRows(), 0L));
                    Set<String> indexedColumns = new HashSet<>();
                    for (IndexDefinition index : table.getIndexes()) {
                        indexedColumns.addAll(index.getColumns().stream()
                                .map(String::toLowerCase)
                                .collect(Collectors.toSet()));
                    }
                    existingIndexes.put(tableName, indexedColumns);
                }
            });

            long largestTableRows = 0L;
            String largestTableName = null;
            int tableVolumeScore = 0;

            for (String rawTableName : parsedQuery.getTables()) {
                String tableName = rawTableName.toLowerCase(Locale.ROOT);
                Long rows = tableRowCounts.get(tableName);
                if (rows == null) {
                    // Table referenced in the query but not found in the selected schema -
                    // could be a typo, a table outside this schema, or a stale schema
                    // definition. Worth flagging rather than silently skipping.
                    tableSizeWarnings.add("Table \"" + rawTableName
                            + "\" was not found in the selected schema - complexity/index analysis for it is schema-blind");
                    continue;
                }
                if (rows > largestTableRows) {
                    largestTableRows = rows;
                    largestTableName = tableName;
                }
                if (rows >= HUGE_TABLE_ROWS) {
                    tableVolumeScore += HUGE_TABLE_POINTS;
                } else if (rows >= LARGE_TABLE_ROWS) {
                    tableVolumeScore += LARGE_TABLE_POINTS;
                }
            }

            if (tableVolumeScore > 0 && largestTableName != null) {
                score += tableVolumeScore;
                factors.add(ComplexityReport.Factor.builder()
                        .name("Table Volume")
                        .count((int) Math.min(largestTableRows, Integer.MAX_VALUE))
                        .points(tableVolumeScore)
                        .description("Largest table involved (" + largestTableName + ") has ~" + largestTableRows
                                + " estimated rows")
                        .build());
            }

            // Filtering/joining on a large table without a matching index is the classic
            // full-table-scan risk - surface it as both score and a warning.
            Set<String> flaggedTables = new HashSet<>();
            for (ParsedQuery.WhereCondition condition : parsedQuery.getWhereConditions()) {
                String table = condition.getTable() == null ? "" : condition.getTable().toLowerCase(Locale.ROOT);
                if (table.isEmpty() && parsedQuery.getTables().size() == 1) {
                    table = parsedQuery.getTables().get(0).toLowerCase(Locale.ROOT);
                }
                if (table.isEmpty()) {
                    continue;
                }
                Long rows = tableRowCounts.get(table);
                Set<String> indexed = existingIndexes.getOrDefault(table, Set.of());
                String column = condition.getColumn() == null ? "" : condition.getColumn().toLowerCase(Locale.ROOT);
                if (rows != null && rows >= LARGE_TABLE_ROWS && !indexed.contains(column)
                        && flaggedTables.add(table)) {
                    score += UNINDEXED_FILTER_ON_LARGE_TABLE_POINTS;
                    tableSizeWarnings.add("Filtering on \"" + table + "\" (~" + rows
                            + " rows) without a matching index - likely full table scan");
                }
            }
        }

        // Cap at 100
        score = Math.min(score, 100);

        // Determine complexity level
        ComplexityReport.Level level = determineLevel(score);

        // Generate warnings based on complexity
        List<String> warnings = generateWarnings(parsedQuery, score);
        warnings.addAll(tableSizeWarnings);

        return ComplexityReport.builder()
                .score(score)
                .level(level.name())
                .factors(factors)
                .warnings(warnings)
                .joinCount(joinCount)
                .subqueryDepth(getMaxSubqueryDepth(parsedQuery))
                .aggregateCount(aggregateCount)
                .build();
    }

    private int calculateSubqueryScore(ParsedQuery query) {
        int score = 0;
        for (ParsedQuery subquery : query.getSubqueries()) {
            int depth = subquery.getSubqueryDepth();
            score += SUBQUERY_POINTS * (depth + 1);
            score += calculateSubqueryScore(subquery);
        }
        return score;
    }

    private int countTotalSubqueries(ParsedQuery query) {
        int count = query.getSubqueries().size();
        for (ParsedQuery subquery : query.getSubqueries()) {
            count += countTotalSubqueries(subquery);
        }
        return count;
    }

    private int getMaxSubqueryDepth(ParsedQuery query) {
        int maxDepth = 0;
        for (ParsedQuery subquery : query.getSubqueries()) {
            maxDepth = Math.max(maxDepth, subquery.getSubqueryDepth() + 1);
            maxDepth = Math.max(maxDepth, getMaxSubqueryDepth(subquery));
        }
        return maxDepth;
    }

    private ComplexityReport.Level determineLevel(int score) {
        if (score <= 25) {
            return ComplexityReport.Level.LOW;
        }
        if (score <= 50) {
            return ComplexityReport.Level.MEDIUM;
        }
        if (score <= 75) {
            return ComplexityReport.Level.HIGH;
        }
        return ComplexityReport.Level.CRITICAL;
    }

    private List<String> generateWarnings(ParsedQuery query, int score) {
        List<String> warnings = new ArrayList<>();

        // Check for SELECT *
        if (query.getColumns().contains("*")) {
            warnings.add("SELECT * detected - specify columns explicitly for better performance");
        }

        // Too many joins
        if (query.getJoins().size() > 4) {
            warnings.add(
                    "High number of JOINs (" + query.getJoins().size() + ") - consider breaking into multiple queries");
        }

        // Deep subqueries
        int maxDepth = getMaxSubqueryDepth(query);
        if (maxDepth > 2) {
            warnings.add("Deeply nested subqueries (depth: " + maxDepth + ") - consider using CTEs or JOINs");
        }

        // High complexity score
        if (score > 75) {
            warnings.add("Query complexity is CRITICAL - review for optimization opportunities");
        }

        // Check for non-parameterized values that look like user input
        for (ParsedQuery.WhereCondition condition : query.getWhereConditions()) {
            if (!condition.isParameterized() && condition.getValue() != null
                    && !condition.getValue().matches("^[0-9]+$")
                    && !condition.getValue().equals("NULL")
                    && !condition.getValue().equals("TRUE")
                    && !condition.getValue().equals("FALSE")) {
                warnings.add("Potential security issue: non-parameterized value in WHERE clause");
                break;
            }
        }

        return warnings;
    }
}

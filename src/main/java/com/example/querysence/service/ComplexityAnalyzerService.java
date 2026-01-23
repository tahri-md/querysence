package com.example.querysence.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.example.querysence.model.dto.ComplexityReport;
import com.example.querysence.parser.ParsedQuery;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ComplexityAnalyzerService {

    private static final int BASE_SCORE = 10;
    private static final int JOIN_POINTS = 8;
    private static final int SUBQUERY_POINTS = 12;
    private static final int AGGREGATE_POINTS = 3;
    private static final int UNION_POINTS = 10;
    private static final int CASE_POINTS = 5;
    private static final int WINDOW_FUNCTION_POINTS = 7;
    private static final int DISTINCT_POINTS = 3;
    private static final int GROUP_BY_HAVING_POINTS = 5;
    private static final int EXTRA_WHERE_CONDITION_POINTS = 2;
    private static final int WHERE_THRESHOLD = 5;

    public ComplexityReport analyze(ParsedQuery parsedQuery) {
        List<ComplexityReport.Factor> factors = new ArrayList<>();
        int score = BASE_SCORE;

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
        }

        // Extra WHERE conditions beyond threshold
        int whereCount = parsedQuery.getWhereConditions().size();
        if (whereCount > WHERE_THRESHOLD) {
            int extraConditions = whereCount - WHERE_THRESHOLD;
            int extraScore = extraConditions * EXTRA_WHERE_CONDITION_POINTS;
            score += extraScore;
            factors.add(ComplexityReport.Factor.builder()
                    .name("WHERE Conditions")
                    .count(whereCount)
                    .points(extraScore)
                    .description(whereCount + " conditions (>" + WHERE_THRESHOLD + " threshold)")
                    .build());
        }

        // Cap at 100
        score = Math.min(score, 100);

        // Determine complexity level
        ComplexityReport.Level level = determineLevel(score);

        // Generate warnings based on complexity
        List<String> warnings = generateWarnings(parsedQuery, score);

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
        if (score <= 25) return ComplexityReport.Level.LOW;
        if (score <= 50) return ComplexityReport.Level.MEDIUM;
        if (score <= 75) return ComplexityReport.Level.HIGH;
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
            warnings.add("High number of JOINs (" + query.getJoins().size() + ") - consider breaking into multiple queries");
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

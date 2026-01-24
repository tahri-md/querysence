package com.example.querysence.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.example.querysence.exception.InvalidSQLException;
import com.example.querysence.model.QueryParseResponse;
import com.example.querysence.parser.ParsedQuery;
import com.example.querysence.parser.SQLParserEngine;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueryParserService {

    private final SQLParserEngine parserEngine;

    public ParsedQuery parseQuery(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            throw new InvalidSQLException("SQL query cannot be empty");
        }

        ParsedQuery result = parserEngine.parse(sql.trim());

        if (!result.isValid()) {
            throw new InvalidSQLException(result.getErrorMessage());
        }

        return result;
    }

    public QueryParseResponse parseAndFormat(String sql, String dialect) {
        ParsedQuery parsed = parseQuery(sql);

        return QueryParseResponse.builder()
                .valid(parsed.isValid())
                .queryType(parsed.getQueryType())
                .tables(parsed.getTables())
                .columns(parsed.getColumns())
                .joins(parsed.getJoins().stream()
                        .map(j -> QueryParseResponse.JoinResponse.builder()
                                .type(j.getType())
                                .table(j.getTable())
                                .alias(j.getAlias())
                                .condition(j.getCondition())
                                .build())
                        .toList())
                .whereConditions(parsed.getWhereConditions().stream()
                        .map(w -> QueryParseResponse.WhereConditionResponse.builder()
                                .column(w.getColumn())
                                .table(w.getTable())
                                .operator(w.getOperator())
                                .value(w.getValue())
                                .isParameterized(w.isParameterized())
                                .build())
                        .toList())
                .orderBy(parsed.getOrderByColumns())
                .groupBy(parsed.getGroupByColumns())
                .subqueryCount(countSubqueries(parsed))
                .hasDistinct(parsed.isHasDistinct())
                .hasHaving(parsed.isHasHaving())
                .aggregateFunctions(parsed.getAggregateFunctions())
                .build();
    }

    public String computeQueryHash(String sql) {
        try {
            String normalized = normalizeSql(sql);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(normalized.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    private String normalizeSql(String sql) {
        return sql.trim()
                .toLowerCase()
                .replaceAll("\\s+", " ")
                .replaceAll("\\s*,\\s*", ",")
                .replaceAll("\\s*=\\s*", "=");
    }

    private int countSubqueries(ParsedQuery parsed) {
        int count = parsed.getSubqueries().size();
        for (ParsedQuery subquery : parsed.getSubqueries()) {
            count += countSubqueries(subquery);
        }
        return count;
    }
}

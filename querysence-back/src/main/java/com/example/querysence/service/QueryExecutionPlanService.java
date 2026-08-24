package com.example.querysence.service;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.example.querysence.exception.BadRequestException;
import com.example.querysence.model.DbConnection;
import com.example.querysence.model.ExecutionPlan;
import com.example.querysence.model.PlanSource;
import com.example.querysence.model.QueryHistory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Service
@RequiredArgsConstructor
@Slf4j
public class QueryExecutionPlanService {

    private final DbConnectionService dbConnectionService;

    private static final Pattern PG_COST_PATTERN =
            Pattern.compile("cost=[\\d.]+\\.\\.([\\d.]+)\\s+rows=(\\d+)");
    private static final Pattern PG_ACTUAL_PATTERN =
            Pattern.compile("actual time=[\\d.]+\\.\\.([\\d.]+)\\s+rows=(\\d+)");
    private static final Pattern SEQ_SCAN_PATTERN =
            Pattern.compile("Seq Scan on (\\w+)");
    private static final Pattern INDEX_SCAN_PATTERN =
            Pattern.compile("Index(?:\\s+Only)? Scan.*? on \\w+ using (\\w+)");

    public ExecutionPlan runLivePlan(QueryHistory history, DbConnection connection, String sql, String queryType) {
        boolean isReadOnly = "SELECT".equalsIgnoreCase(queryType);
        boolean canAnalyze = isReadOnly || !Boolean.TRUE.equals(connection.getReadOnlyEnforced());

        String explainSql = (canAnalyze ? "EXPLAIN (ANALYZE, FORMAT TEXT) " : "EXPLAIN (FORMAT TEXT) ") + sql;

        List<String> planLines = new ArrayList<>();
        try (Connection conn = dbConnectionService.openConnection(connection);
             Statement stmt = conn.createStatement()) {

            if (!canAnalyze) {
                log.info("Connection {} is read-only enforced and query is non-SELECT; running plain EXPLAIN", connection.getId());
            }

            try (ResultSet rs = stmt.executeQuery(explainSql)) {
                while (rs.next()) {
                    planLines.add(rs.getString(1));
                }
            }
        } catch (SQLException e) {
            throw new BadRequestException("Failed to run EXPLAIN against the connected database: " + e.getMessage(), e);
        }

        String planText = String.join("\n", planLines);
        return parsePlan(history, connection, planText, canAnalyze);
    }

    private ExecutionPlan parsePlan(QueryHistory history, DbConnection connection, String planText, boolean isAnalyzed) {
        Double estimatedCost = null;
        Long actualRows = null;
        Double actualTimeMs = null;
        List<String> fullTableScans = new ArrayList<>();
        List<String> usedIndexes = new ArrayList<>();

        for (String line : planText.split("\n")) {
            Matcher costMatcher = PG_COST_PATTERN.matcher(line);
            if (costMatcher.find()) {
                estimatedCost = Double.parseDouble(costMatcher.group(1));
            }

            if (isAnalyzed) {
                Matcher actualMatcher = PG_ACTUAL_PATTERN.matcher(line);
                if (actualMatcher.find()) {
                    actualTimeMs = Double.parseDouble(actualMatcher.group(1));
                    actualRows = Long.parseLong(actualMatcher.group(2));
                }
            }

            Matcher seqScanMatcher = SEQ_SCAN_PATTERN.matcher(line);
            if (seqScanMatcher.find()) {
                fullTableScans.add(seqScanMatcher.group(1));
            }

            Matcher indexScanMatcher = INDEX_SCAN_PATTERN.matcher(line);
            if (indexScanMatcher.find()) {
                usedIndexes.add(indexScanMatcher.group(1));
            }
        }

        return ExecutionPlan.builder()
                .queryHistory(history)
                .dbConnection(connection)
                .source(PlanSource.LIVE_EXPLAIN)
                .planText(planText)
                .estimatedCost(estimatedCost)
                .actualRows(actualRows)
                .actualTimeMs(actualTimeMs)
                .usedIndexes(usedIndexes)
                .fullTableScans(fullTableScans)
                .build();
    }
}

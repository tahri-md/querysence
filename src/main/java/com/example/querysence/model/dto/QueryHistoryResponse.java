package com.example.querysence.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

import com.example.querysence.model.SecurityFinding.Severity;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryHistoryResponse {
    
    private Long id;
    private String queryText;
    private String queryType;
    private Integer complexityScore;
    private Long executionTimeMs;
    private Long projectId;
    private String projectName;
    private LocalDateTime analyzedAt;
    
    private List<IndexSuggestionSummary> indexSuggestions;
    private List<SecurityFindingSummary> securityFindings;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IndexSuggestionSummary {
        private String tableName;
        private List<String> columns;
        private String impactScore;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SecurityFindingSummary {
        private String type;
        private String severity;
        private String description;
    }
}

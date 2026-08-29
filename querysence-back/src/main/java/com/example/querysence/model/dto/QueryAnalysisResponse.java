package com.example.querysence.model.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryAnalysisResponse {

    private Long queryId;
    private String queryType;
    private ComplexityReport complexity;
    private List<IndexSuggestionResponse> indexSuggestions;
    private List<String> warnings;
    private com.example.querysence.model.QueryParseResponse parseResult;

    //  null if analyzed statically (no dbConnectionId supplied in the request)
    private Long dbConnectionId;
    private ExecutionPlanDto executionPlan;

    private LocalDateTime analyzedAt;
}

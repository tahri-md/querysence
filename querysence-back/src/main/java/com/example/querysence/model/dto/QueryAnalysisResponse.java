package com.example.querysence.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
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
    private LocalDateTime analyzedAt;
}

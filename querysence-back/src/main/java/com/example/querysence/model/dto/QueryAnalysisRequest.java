package com.example.querysence.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryAnalysisRequest {
    
    private String sql;
    
    private Long schemaId;
    private Long projectId;
    private Long executionTimeMs;
}

package com.example.querysence.model.dto;

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
public class QueryAnalysisRequest {

    private String sql;
    private Long schemaId;
    private Long projectId;
    private Long executionTimeMs;
    private Long dbConnectionId;
}

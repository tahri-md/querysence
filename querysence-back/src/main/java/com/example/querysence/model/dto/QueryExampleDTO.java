package com.example.querysence.model.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QueryExampleDTO {
    private Long id;
    private Long userId;
    private Long schemaId;
    private String nlQuery;
    private String sqlOutput;
    private float[] embedding;
    private String queryType;
    private Double confidenceScore;
    private Integer tokenCount;
    private Long executionTimeMs;
    private Boolean verified;
    private String vectorId;
    private Double similarity;
    private Integer accessCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

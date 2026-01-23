package com.example.querysence.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptimizationResponse {
    
    private List<Suggestion> suggestions;
    private String overallAssessment;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Suggestion {
        private String type;         // REWRITE, INDEX, STRUCTURE, WARNING
        private String priority;     // HIGH, MEDIUM, LOW
        private String original;
        private String optimized;
        private String explanation;
        private String estimatedImprovement;
    }
}

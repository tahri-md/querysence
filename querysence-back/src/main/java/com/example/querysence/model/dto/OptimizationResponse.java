package com.example.querysence.model.dto;

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
public class OptimizationResponse {
    
    private List<Suggestion> suggestions;
    private String overallAssessment;

    @Getter
    @Setter
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

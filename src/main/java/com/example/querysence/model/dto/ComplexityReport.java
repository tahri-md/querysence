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
public class ComplexityReport {
    
    private int score;
    private String level;
    private List<Factor> factors;
    private List<String> warnings;
    private int joinCount;
    private int subqueryDepth;
    private int aggregateCount;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Factor {
        private String name;
        private int count;
        private int points;
        private String description;
    }

    public enum Level {
        LOW,      // 0-25
        MEDIUM,   // 26-50
        HIGH,     // 51-75
        CRITICAL  // 76-100
    }
}

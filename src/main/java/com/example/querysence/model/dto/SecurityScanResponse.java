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
public class SecurityScanResponse {
    
    private List<Finding> findings;
    private int riskScore;
    private String summary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Finding {
        private String type;          // SQL_INJECTION, MISSING_PARAMETERIZATION, etc.
        private String severity;      // CRITICAL, HIGH, MEDIUM, LOW
        private Integer line;
        private String description;
        private String recommendation;
        private String secureExample;
    }
}

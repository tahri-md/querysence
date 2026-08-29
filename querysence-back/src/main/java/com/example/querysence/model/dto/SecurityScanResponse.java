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
public class SecurityScanResponse {
    
    private List<Finding> findings;
    private int riskScore;
    private String summary;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Finding {
        private String type;          // SQL_INJECTION, MISSING_PARAMETERIZATION, etc.
        private String severity;      // CRITICAL, HIGH, MEDIUM, LOW
        private String line;
        private String description;
        private String recommendation;
        private String secureExample;
    }
}

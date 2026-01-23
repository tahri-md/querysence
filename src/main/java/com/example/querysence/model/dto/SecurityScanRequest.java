package com.example.querysence.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityScanRequest {
    
    private String code;
    
    @Builder.Default
    private String context = "RAW_SQL";  // JAVA, PYTHON, JAVASCRIPT, RAW_SQL
}

package com.example.querysence.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NLToSQLResponse {
    
    private String sql;
    private boolean valid;
    private String errorMessage;
    private String dialect;
    private double confidence;
}

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
public class NLToSQLResponse {
    
    private String sql;
    private boolean valid;
    private String errorMessage;
    private String dialect;
    private double confidence;
}

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
public class IndexSuggestionResponse {
    
    private String tableName;
    private List<String> columns;
    private String indexName;
    private String suggestionType;  // SINGLE, COMPOSITE
    private String impactScore;     // HIGH, MEDIUM, LOW
    private String reasoning;
    private String createStatement;
}

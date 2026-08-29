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
public class IndexSuggestionResponse {
    
    private String tableName;
    private List<String> columns;
    private String indexName;
    private String suggestionType;  // SINGLE, COMPOSITE
    private String impactScore;     // HIGH, MEDIUM, LOW
    private String reasoning;
    private String createStatement;
}

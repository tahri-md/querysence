package com.example.querysence.model.dto;

import java.util.ArrayList;
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
public class TableDefinitionRequest {
      
    private String tableName;
    
    @Builder.Default
    private Long estimatedRows = 0L;
    
    private String description;
    
    @Builder.Default
    private List<ColumnDefinitionRequest> columns = new ArrayList<>();
    
    @Builder.Default
    private List<IndexDefinitionRequest> indexes = new ArrayList<>();

}
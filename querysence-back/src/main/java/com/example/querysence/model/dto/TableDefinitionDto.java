package com.example.querysence.model.dto;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.example.querysence.model.ColumnDefinition;
import com.example.querysence.model.IndexDefinition;

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
public class TableDefinitionDto {
  
    private Long id;
    
    private String tableName;
    
    @Builder.Default
    private Long estimatedRows = 0L;
    
    private String description;
    
    @Builder.Default
    private Set<ColumnDefinition> columns = new HashSet<>();
    
    @Builder.Default
    private List<IndexDefinition> indexes = new ArrayList<>();

}
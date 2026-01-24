package com.example.querysence.model.dto;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import com.example.querysence.model.ColumnDefinition;
import com.example.querysence.model.IndexDefinition;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableDefinitionDto {
  
    private Long id;
    
    private String tableName;
    
    @Builder.Default
    private Long estimatedRows = 0L;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Builder.Default
    private Set<ColumnDefinition> columns = new HashSet<>();
    
    @Builder.Default
    private List<IndexDefinition> indexes = new ArrayList<>();

}
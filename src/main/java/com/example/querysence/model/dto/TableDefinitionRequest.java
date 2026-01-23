package com.example.querysence.model.dto;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableDefinitionRequest {
  
    private Long id;
    
    private String tableName;
    
    @Builder.Default
    private Long estimatedRows = 0L;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Builder.Default
    private List<ColumnDefinitionRequest> columns = new ArrayList<>();
    
    @Builder.Default
    private List<IndexDefinitionRequest> indexes = new ArrayList<>();

}
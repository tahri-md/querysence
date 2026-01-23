package com.example.querysence.model.dto;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ColumnDefinitionRequest {
    
    private String columnName;
    
    private String dataType;
    
    @Builder.Default
    private Boolean isNullable = true;
    
    @Builder.Default
    private Boolean isPrimaryKey = false;
    
    @Builder.Default
    private Boolean isForeignKey = false;
    
    private String referencesTable;
    
    private String referencesColumn;

}
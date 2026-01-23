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
public class IndexDefinitionDto {
    
    
    private String indexName;
    
    private String[] columns;
    
    @Builder.Default
    private Boolean isUnique = false;
    
    @Builder.Default
    private String indexType = "BTREE";
    
}
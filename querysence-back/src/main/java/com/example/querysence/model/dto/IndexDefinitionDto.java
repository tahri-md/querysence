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
public class IndexDefinitionDto {
    
    
    private String indexName;
    
    private String[] columns;
    
    @Builder.Default
    private Boolean isUnique = false;
    
    @Builder.Default
    private String indexType = "BTREE";
    
}
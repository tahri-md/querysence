package com.example.querysence.model.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchemaCreateRequest {
    
   
    private String name;
    
    @Builder.Default
    private String dialect = "POSTGRESQL";
    
    private String ddlScript;
}

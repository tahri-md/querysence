package com.example.querysence.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectResponse {
    
    private Long id;
    private String name;
    private String description;
    private int schemaCount;
    private List<SchemaResponse> schemas;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

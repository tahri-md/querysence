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

    //  dev/staging/prod connections under this project (empty list if none — manual-only project)
    @Builder.Default
    private List<DbConnectionDto> dbConnections = new java.util.ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

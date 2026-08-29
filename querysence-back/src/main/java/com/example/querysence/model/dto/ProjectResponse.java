package com.example.querysence.model.dto;

import java.time.LocalDateTime;
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

package com.example.querysence.model.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
public class SchemaDefinitionDto {

    private String name;

    @Builder.Default
    private String dialect = "POSTGRESQL";

    // "MANUAL" or "SYNCED"
    @Builder.Default
    private String source = "MANUAL";

    // only present when source = "SYNCED"
    private Long dbConnectionId;

    @Builder.Default
    private List<TableDefinitionDto> tables = new ArrayList<>();

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

package com.example.querysence.model.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchemaDefinitionDto {

    private String name;

    @Builder.Default
    private String dialect = "POSTGRESQL";

    //  "MANUAL" or "SYNCED"
    @Builder.Default
    private String source = "MANUAL";

    //  only present when source = "SYNCED"
    private Long dbConnectionId;

    @Builder.Default
    private List<TableDefinitionDto> tables = new ArrayList<>();

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

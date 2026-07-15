package com.example.querysence.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchemaSyncResponse {

    private Long syncLogId;
    private Long schemaId;
    private String status;      // RUNNING, SUCCESS, FAILED
    private Integer tablesDiscovered;
    private Integer columnsDiscovered;
    private Integer indexesDiscovered;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}

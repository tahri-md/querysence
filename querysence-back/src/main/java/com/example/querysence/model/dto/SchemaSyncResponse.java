package com.example.querysence.model.dto;

import java.time.LocalDateTime;

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

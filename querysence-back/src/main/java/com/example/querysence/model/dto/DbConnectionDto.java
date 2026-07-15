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
public class DbConnectionDto {

    private Long id;
    private Long projectId;
    private String name;
    private String host;
    private Integer port;
    private String databaseName;
    private String username;
    // password intentionally omitted
    private String dialect;
    private Boolean sslEnabled;
    private Boolean readOnlyEnforced;
    private String status;
    private LocalDateTime lastTestedAt;
    private LocalDateTime lastSyncedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

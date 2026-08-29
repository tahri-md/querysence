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
public class DbConnectionDto {

    private Long id;
    private Long projectId;
    private String name;
    private String host;
    private Integer port;
    private String databaseName;
    private String username;
    private String dialect;
    private Boolean sslEnabled;
    private Boolean readOnlyEnforced;
    private String status;
    private LocalDateTime lastTestedAt;
    private LocalDateTime lastSyncedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

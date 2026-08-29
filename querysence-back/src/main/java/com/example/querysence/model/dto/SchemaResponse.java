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
public class SchemaResponse {

    private Long id;
    private String name;
    private String dialect;
    private Long projectId;

    private String source; // "MANUAL" or "SYNCED"
    private Long dbConnectionId; // null when source = "MANUAL"
    private LocalDateTime lastSyncedAt; // null when source = "MANUAL"

    private List<TableResponse> tables;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TableResponse {
        private Long id;
        private String tableName;
        private Long estimatedRows;
        private String description;
        private List<ColumnResponse> columns;
        private List<IndexResponse> indexes;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ColumnResponse {
        private Long id;
        private String columnName;
        private String dataType;
        private Boolean isNullable;
        private Boolean isPrimaryKey;
        private Boolean isForeignKey;
        private String referencesTable;
        private String referencesColumn;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IndexResponse {
        private Long id;
        private String indexName;
        private List<String> columns;
        private Boolean isUnique;
        private String indexType;
    }
}

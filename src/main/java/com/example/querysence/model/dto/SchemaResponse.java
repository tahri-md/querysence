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
public class SchemaResponse {
    
    private Long id;
    private String name;
    private String dialect;
    private Long projectId;
    private List<TableResponse> tables;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
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

    @Data
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

    @Data
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

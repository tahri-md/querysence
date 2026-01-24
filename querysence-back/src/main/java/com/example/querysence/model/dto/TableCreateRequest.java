package com.example.querysence.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableCreateRequest {
    
    private String tableName;
    
    private Long estimatedRows;
    private String description;
    private List<ColumnRequest> columns;
    private List<IndexRequest> indexes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ColumnRequest {
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
    public static class IndexRequest {
        private String indexName;
        
        private List<String> columns;
        private Boolean isUnique;
        private String indexType;
    }
}

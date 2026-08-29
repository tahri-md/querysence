package com.example.querysence.model.dto;

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
public class TableCreateRequest {
    
    private String tableName;
    
    private Long estimatedRows;
    private String description;
    private List<ColumnRequest> columns;
    private List<IndexRequest> indexes;

    @Getter
    @Setter
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

    @Getter
    @Setter
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

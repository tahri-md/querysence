package com.example.querysence.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryParseResponse {
    
    private boolean valid;
    private String queryType;
    private List<String> tables;
    private List<String> columns;
    private List<JoinResponse> joins;
    private List<WhereConditionResponse> whereConditions;
    private List<String> orderBy;
    private List<String> groupBy;
    private int subqueryCount;
    private boolean hasDistinct;
    private boolean hasHaving;
    private List<String> aggregateFunctions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JoinResponse {
        private String type;
        private String table;
        private String alias;
        private String condition;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WhereConditionResponse {
        private String column;
        private String table;
        private String operator;
        private String value;
        private boolean isParameterized;
    }
}

package com.example.querysence.parser;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParsedQuery {
    
    private boolean valid;
    private String queryType;
    private String errorMessage;
    
    @Builder.Default
    private List<String> tables = new ArrayList<>();
    
    @Builder.Default
    private List<String> columns = new ArrayList<>();
    
    @Builder.Default
    private List<JoinInfo> joins = new ArrayList<>();
    
    @Builder.Default
    private List<WhereCondition> whereConditions = new ArrayList<>();
    
    @Builder.Default
    private List<String> orderByColumns = new ArrayList<>();
    
    @Builder.Default
    private List<String> groupByColumns = new ArrayList<>();
    
    @Builder.Default
    private List<ParsedQuery> subqueries = new ArrayList<>();
    
    @Builder.Default
    private List<String> aggregateFunctions = new ArrayList<>();
    
    private boolean hasDistinct;
    private boolean hasHaving;
    private int subqueryDepth;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JoinInfo {
        private String type;
        private String table;
        private String alias;
        private String condition;
        private List<String> joinColumns;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WhereCondition {
        private String column;
        private String table;
        private String operator;
        private String value;
        private boolean isParameterized;
    }
}

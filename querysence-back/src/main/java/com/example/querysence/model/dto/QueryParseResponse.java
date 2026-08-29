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
    private List<FunctionResponse> functions;
    private List<SelectExpressionResponse> selectExpressions;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JoinResponse {
        private String type;
        private String table;
        private String alias;
        private String condition;
    }

    @Getter
    @Setter
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

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FunctionResponse {
        private String name;
        private String category;
        private String expression;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SelectExpressionResponse {
        private String type;
        private String text;
        private String alias;
        private String operator;
        private String value;
        private List<SelectExpressionResponse> children;
    }
}

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

    @Builder.Default
    private java.util.Map<String, String> aliasMap = new java.util.HashMap<>();
    
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
        private List<JoinKey> joinKeys;
        private ParsedQuery subquery;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JoinKey {
        private String left;
        private String right;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WhereCondition implements Condition {
        private String column;
        private String table;
        private String operator;
        private String value;
        private boolean isParameterized;
    }

    interface Condition {}

    public static class ComparisonCondition implements Condition {
        public String column;
        public String table;
        public String operator;
        public String value;
        public boolean isParameterized;

        public ComparisonCondition() {
        }

        public ComparisonCondition(String column, String table, String operator, String value, boolean isParameterized) {
            this.column = column;
            this.table = table;
            this.operator = operator;
            this.value = value;
            this.isParameterized = isParameterized;
        }
    }

    public static class InCondition implements Condition {
        public String column;
        public String table;
        public List<String> values;
        public boolean isParameterized;
        public ParsedQuery subquery;

        public InCondition() {
        }

        public InCondition(String column, String table, List<String> values, boolean isParameterized, ParsedQuery subquery) {
            this.column = column;
            this.table = table;
            this.values = values;
            this.isParameterized = isParameterized;
            this.subquery = subquery;
        }
    }

    public static class BetweenCondition implements Condition {
        public String column;
        public String table;
        public String startValue;
        public String endValue;
        public boolean isParameterized;

        public BetweenCondition() {
        }

        public BetweenCondition(String column, String table, String startValue, String endValue, boolean isParameterized) {
            this.column = column;
            this.table = table;
            this.startValue = startValue;
            this.endValue = endValue;
            this.isParameterized = isParameterized;
        }
    }

    public static class LikeCondition implements Condition {
        public String column;
        public String table;
        public String pattern;
        public boolean isParameterized;

        public LikeCondition() {
        }

        public LikeCondition(String column, String table, String pattern, boolean isParameterized) {
            this.column = column;
            this.table = table;
            this.pattern = pattern;
            this.isParameterized = isParameterized;
        }
    }

    public static class IsNullCondition implements Condition {
        public String column;
        public String table;
        public boolean isNot;

        public IsNullCondition() {
        }

        public IsNullCondition(String column, String table, boolean isNot) {
            this.column = column;
            this.table = table;
            this.isNot = isNot;
        }
    }

    public static class ExistsCondition implements Condition {
        public ParsedQuery subquery;

        public ExistsCondition() {
        }

        public ExistsCondition(ParsedQuery subquery) {
            this.subquery = subquery;
        }
    }

    public static class AndCondition implements Condition {
        public Condition left;
        public Condition right;

        public AndCondition() {
        }

        public AndCondition(Condition left, Condition right) {
            this.left = left;
            this.right = right;
        }
    }

    public static class OrCondition implements Condition {
        public Condition left;
        public Condition right;

        public OrCondition() {
        }

        public OrCondition(Condition left, Condition right) {
            this.left = left;
            this.right = right;
        }
    }
}

package com.example.querysence.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
    private List<FunctionInfo> functions = new ArrayList<>();

    @Builder.Default
    private List<SelectExpression> selectExpressions = new ArrayList<>();

    @Builder.Default
    private Map<String, String> aliasMap = new java.util.HashMap<>();

    private boolean hasDistinct;
    private boolean hasHaving;
    private int subqueryDepth;

    @Getter
    @Setter
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

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JoinKey {
        private String left;
        private String right;
    }

    public enum SelectExpressionType {
        COLUMN,
        LITERAL,
        ARITHMETIC,
        CASE,
        FUNCTION,
        PARENTHESIZED,
        SUBQUERY,
        UNKNOWN
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SelectExpression {
        private SelectExpressionType type;
        private String text;
        private String alias;
        private String operator;
        private String value;
        private List<SelectExpression> children;
    }

    public enum FunctionCategory {
        AGGREGATE,
        SCALAR,
        WINDOW,
        CUSTOM
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FunctionInfo {
        private String name;
        private FunctionCategory category;
        private String expression;
    }

    @Getter
    @Setter
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

    interface Condition {
    }

    @Getter
    public static class ComparisonCondition implements Condition {
        private String column;
        private String table;
        private String operator;
        private String value;
        private boolean isParameterized;

        public ComparisonCondition() {
        }

        public ComparisonCondition(String column, String table, String operator, String value,
                boolean isParameterized) {
            this.column = column;
            this.table = table;
            this.operator = operator;
            this.value = value;
            this.isParameterized = isParameterized;
        }
    }

    @Getter
    public static class InCondition implements Condition {
        private String column;
        private String table;
        private List<String> values;
        private boolean isParameterized;
        private ParsedQuery subquery;

        public InCondition() {
        }

        @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "ParsedQuery is an internal parse result; defensive copy not warranted")
        public InCondition(String column, String table, List<String> values, boolean isParameterized,
                ParsedQuery subquery) {
            this.column = column;
            this.table = table;
            this.values = values == null ? null : new ArrayList<>(values);
            this.isParameterized = isParameterized;
            this.subquery = subquery;
        }
    }

    @Getter
    public static class BetweenCondition implements Condition {
        private String column;
        private String table;
        private String startValue;
        private String endValue;
        private boolean isParameterized;

        public BetweenCondition() {
        }

        public BetweenCondition(String column, String table, String startValue, String endValue,
                boolean isParameterized) {
            this.column = column;
            this.table = table;
            this.startValue = startValue;
            this.endValue = endValue;
            this.isParameterized = isParameterized;
        }
    }

    @Getter
    public static class LikeCondition implements Condition {
        private String column;
        private String table;
        private String pattern;
        private boolean isParameterized;

        public LikeCondition() {
        }

        public LikeCondition(String column, String table, String pattern, boolean isParameterized) {
            this.column = column;
            this.table = table;
            this.pattern = pattern;
            this.isParameterized = isParameterized;
        }
    }

    @Getter
    public static class IsNullCondition implements Condition {
        private String column;
        private String table;
        private boolean isNot;

        public IsNullCondition() {
        }

        public IsNullCondition(String column, String table, boolean isNot) {
            this.column = column;
            this.table = table;
            this.isNot = isNot;
        }
    }

    @Getter
    public static class ExistsCondition implements Condition {
        private ParsedQuery subquery;

        public ExistsCondition() {
        }

        @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "ParsedQuery is an internal parse result; defensive copy not warranted")
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

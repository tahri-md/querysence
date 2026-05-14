package com.example.querysence.parser;

import com.example.querysence.exception.*;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.statement.update.Update;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
@Slf4j
public class SQLParserEngine {

    private static final Set<String> AGGREGATE_FUNCTIONS = new HashSet<>(Arrays.asList(
        "COUNT", "SUM", "AVG", "MIN", "MAX", "STDDEV", "STDDEV_POP", "STDDEV_SAMP",
        "VARIANCE", "VAR_POP", "VAR_SAMP", "GROUP_CONCAT", "STRING_AGG", "ARRAY_AGG", "JSON_AGG",
        "BOOL_AND", "BOOL_OR"
    ));

    private static final Set<String> WINDOW_FUNCTIONS = new HashSet<>(Arrays.asList(
        "ROW_NUMBER", "RANK", "DENSE_RANK", "PERCENT_RANK", "CUME_DIST", "NTILE",
        "LAG", "LEAD", "FIRST_VALUE", "LAST_VALUE", "NTH_VALUE"
    ));

    private static final Set<String> SCALAR_FUNCTIONS = new HashSet<>(Arrays.asList(
        "LOWER", "UPPER", "ROUND", "ABS", "TRIM", "LTRIM", "RTRIM", "COALESCE",
        "CONCAT", "LENGTH", "SUBSTRING", "SUBSTR", "REPLACE", "CAST", "CEIL", "FLOOR",
        "LEFT", "RIGHT", "POSITION", "NOW", "CURRENT_DATE", "CURRENT_TIMESTAMP"
    ));

    public ParsedQuery parse(String sql) {
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            return parseStatement(statement, 0);
        } catch (JSQLParserException e) {
            log.error("Failed to parse SQL: {}", e.getMessage());
            return ParsedQuery.builder()
                    .valid(false)
                    .errorMessage("Invalid SQL syntax: " + e.getMessage())
                    .build();
        }
    }

    private ParsedQuery parseStatement(Statement statement, int depth) {
        ParsedQuery.ParsedQueryBuilder builder = ParsedQuery.builder()
                .valid(true)
                .subqueryDepth(depth);

        if (statement instanceof Select select) {
            builder.queryType("SELECT");
            parseSelect(select, builder, depth);
        } else if (statement instanceof Insert insert) {
            builder.queryType("INSERT");
            parseInsert(insert, builder);
        } else if (statement instanceof Update update) {
            builder.queryType("UPDATE");
            parseUpdate(update, builder);
        } else if (statement instanceof Delete delete) {
            builder.queryType("DELETE");
            parseDelete(delete, builder);
        } else {
            throw new InvalidSQLException("Unsupported SQL statement type: " + statement.getClass().getSimpleName());
        }

        return builder.build();
    }

    private void parseSelect(Select select, ParsedQuery.ParsedQueryBuilder builder, int depth) {
        PlainSelect plainSelect = select.getPlainSelect();
        if (plainSelect == null) {
            return;
        }

        // Parse SELECT columns
        List<String> columns = new ArrayList<>();
        List<String> aggregates = new ArrayList<>();
        List<ParsedQuery.FunctionInfo> functions = new ArrayList<>();

        for (SelectItem<?> item : plainSelect.getSelectItems()) {
            if (item.getExpression() instanceof AllColumns) {
                columns.add("*");
            } else if (item.getExpression() instanceof AllTableColumns atc) {
                columns.add(atc.getTable().getName() + ".*");
            } else if (item.getExpression() instanceof Column col) {
                columns.add(col.getColumnName());
            } else if (item.getExpression() instanceof Function func) {
                collectFunctionUsage(func, functions, aggregates);
                columns.add(func.toString());
            } else {
                collectFunctionsFromExpression(item.getExpression(), functions, aggregates);
                columns.add(item.toString());
            }
        }
        builder.columns(columns);
        builder.aggregateFunctions(aggregates);
        builder.functions(functions);
        builder.hasDistinct(plainSelect.getDistinct() != null);

        // Parse FROM clause
        List<String> tables = new ArrayList<>();
        java.util.Map<String,String> aliasMap = new java.util.HashMap<>();
        if (plainSelect.getFromItem() != null) {
            extractTables(plainSelect.getFromItem(), tables, builder, aliasMap);
        }
        builder.tables(tables);
        builder.aliasMap(aliasMap);

        // Parse JOINs
        List<ParsedQuery.JoinInfo> joins = new ArrayList<>();
        if (plainSelect.getJoins() != null) {
            for (Join join : plainSelect.getJoins()) {
                ParsedQuery.JoinInfo joinInfo = parseJoin(join, aliasMap);
                joins.add(joinInfo);
                if (join.getFromItem() instanceof Table table) {
                    tables.add(table.getName());
                }
            }
        }
        builder.joins(joins);

        // Parse WHERE clause
        List<ParsedQuery> subqueries = new ArrayList<>();
        ParsedQuery.Condition whereCondition = null;
        if (plainSelect.getWhere() != null) {
            whereCondition = parseWhereExpression(plainSelect.getWhere(), subqueries, depth);
            collectFunctionsFromExpression(plainSelect.getWhere(), functions, aggregates);
        }
        if (whereCondition != null) {
            builder.whereConditions(flattenConditions(whereCondition));
        }
        builder.subqueries(subqueries);

        // Parse GROUP BY
        if (plainSelect.getGroupBy() != null) {
            List<String> groupByCols = new ArrayList<>();
            for (Object obj : plainSelect.getGroupBy().getGroupByExpressionList()) {
                Expression expr = (Expression) obj;
                if (expr instanceof Column col) {
                    groupByCols.add(col.getColumnName());
                }
            }
            builder.groupByColumns(groupByCols);
        }

        // Parse HAVING
        builder.hasHaving(plainSelect.getHaving() != null);
        if (plainSelect.getHaving() != null) {
            collectFunctionsFromExpression(plainSelect.getHaving(), functions, aggregates);
        }

        // Parse ORDER BY
        if (plainSelect.getOrderByElements() != null) {
            List<String> orderByCols = new ArrayList<>();
            plainSelect.getOrderByElements().forEach(elem -> {
                if (elem.getExpression() instanceof Column col) {
                    orderByCols.add(col.getColumnName());
                }
                collectFunctionsFromExpression(elem.getExpression(), functions, aggregates);
            });
            builder.orderByColumns(orderByCols);
        }

        builder.aggregateFunctions(aggregates);
        builder.functions(functions);
    }

    private void collectFunctionsFromExpression(Expression expression,
                                                List<ParsedQuery.FunctionInfo> functions,
                                                List<String> aggregateNames) {
        if (expression == null) {
            return;
        }

        if (expression instanceof Function function) {
            collectFunctionUsage(function, functions, aggregateNames);
            return;
        }

        if (expression instanceof BinaryExpression binaryExpression) {
            collectFunctionsFromExpression(binaryExpression.getLeftExpression(), functions, aggregateNames);
            collectFunctionsFromExpression(binaryExpression.getRightExpression(), functions, aggregateNames);
            return;
        }

        if (expression instanceof CaseExpression caseExpression) {
            if (caseExpression.getSwitchExpression() != null) {
                collectFunctionsFromExpression(caseExpression.getSwitchExpression(), functions, aggregateNames);
            }
            if (caseExpression.getWhenClauses() != null) {
                caseExpression.getWhenClauses().forEach(whenClause -> {
                    collectFunctionsFromExpression(whenClause.getWhenExpression(), functions, aggregateNames);
                    collectFunctionsFromExpression(whenClause.getThenExpression(), functions, aggregateNames);
                });
            }
            collectFunctionsFromExpression(caseExpression.getElseExpression(), functions, aggregateNames);
            return;
        }

        if (expression.getClass().getSimpleName().equals("ParenthesedExpressionList")) {
            extractExpressionListFromObject(expression, functions, aggregateNames);
        }
    }

    private void collectFunctionUsage(Function function,
                                     List<ParsedQuery.FunctionInfo> functions,
                                     List<String> aggregateNames) {
        if (function == null) {
            return;
        }

        String name = function.getName();
        if (name == null || name.isBlank()) {
            name = function.toString();
        }

        ParsedQuery.FunctionCategory category = classifyFunction(function);
        functions.add(ParsedQuery.FunctionInfo.builder()
                .name(name)
                .category(category)
                .expression(function.toString())
                .build());

        if (category == ParsedQuery.FunctionCategory.AGGREGATE) {
            aggregateNames.add(name);
        }

        try {
            java.lang.reflect.Method parametersMethod = function.getClass().getMethod("getParameters");
            Object parameters = parametersMethod.invoke(function);
            extractExpressionListFromObject(parameters, functions, aggregateNames);
        } catch (NoSuchMethodException ignored) {
        } catch (Exception e) {
            log.debug("Failed to inspect function parameters for nested functions: {}", e.getMessage());
        }
    }

    private void extractExpressionListFromObject(Object candidate,
                                                 List<ParsedQuery.FunctionInfo> functions,
                                                 List<String> aggregateNames) {
        if (candidate == null) {
            return;
        }

        try {
            java.lang.reflect.Method expressionsMethod = candidate.getClass().getMethod("getExpressions");
            Object expressions = expressionsMethod.invoke(candidate);
            if (expressions instanceof java.util.List<?> list) {
                for (Object item : list) {
                    if (item instanceof Expression itemExpression) {
                        collectFunctionsFromExpression(itemExpression, functions, aggregateNames);
                    }
                }
                return;
            }
        } catch (NoSuchMethodException ignored) {
        } catch (Exception e) {
            log.debug("Failed to inspect expression list for nested functions: {}", e.getMessage());
        }

        if (candidate instanceof Expression expression) {
            collectFunctionsFromExpression(expression, functions, aggregateNames);
        }
    }

    private ParsedQuery.FunctionCategory classifyFunction(Function function) {
        if (function == null) {
            return ParsedQuery.FunctionCategory.CUSTOM;
        }

        String name = function.getName();
        if (name == null) {
            return ParsedQuery.FunctionCategory.CUSTOM;
        }

        String normalizedName = name.toUpperCase(Locale.ROOT);
        String expression = function.toString().toUpperCase(Locale.ROOT);

        if (expression.contains(" OVER ") || WINDOW_FUNCTIONS.contains(normalizedName)) {
            return ParsedQuery.FunctionCategory.WINDOW;
        }

        if (AGGREGATE_FUNCTIONS.contains(normalizedName)) {
            return ParsedQuery.FunctionCategory.AGGREGATE;
        }

        if (SCALAR_FUNCTIONS.contains(normalizedName)) {
            return ParsedQuery.FunctionCategory.SCALAR;
        }

        return ParsedQuery.FunctionCategory.CUSTOM;
    }

    private void extractTables(FromItem fromItem, List<String> tables, ParsedQuery.ParsedQueryBuilder builder, java.util.Map<String,String> aliasMap) {
        if (fromItem instanceof Table table) {
            tables.add(table.getName());
            if (table.getAlias() != null && table.getAlias().getName() != null) {
                aliasMap.put(table.getAlias().getName(), table.getName());
            }
        } else {
            // handle subselects or other from-item types by parsing their SQL
            try {
                ParsedQuery subquery = parse(fromItem.toString());
                List<ParsedQuery> subqueries = builder.build().getSubqueries();
                if (subqueries == null)
                    subqueries = new ArrayList<>();
                subqueries.add(subquery);
                builder.subqueries(subqueries);
                if (fromItem.getAlias() != null && fromItem.getAlias().getName() != null) {
                    aliasMap.put(fromItem.getAlias().getName(), "SUBQUERY");
                }
            } catch (Exception e) {
                log.debug("Failed to parse from-item as subquery: {}", e.getMessage());
            }
        }
    }

    private ParsedQuery.JoinInfo parseJoin(Join join, java.util.Map<String,String> aliasMap) {
        String joinType = "INNER";
        if (join.isLeft())
            joinType = "LEFT";
        else if (join.isRight())
            joinType = "RIGHT";
        else if (join.isFull())
            joinType = "FULL";
        else if (join.isCross())
            joinType = "CROSS";

        String tableName = "";
        String alias = "";
        ParsedQuery subquery = null;
        FromItem fromItem = join.getFromItem();
        if (fromItem instanceof Table table) {
            tableName = table.getName();
            alias = table.getAlias() != null ? table.getAlias().getName() : "";
            if (table.getAlias() != null && table.getAlias().getName() != null) {
                aliasMap.put(table.getAlias().getName(), table.getName());
            }
        } else if (fromItem.getClass().getSimpleName().equals("SubSelect")) {
            alias = fromItem.getAlias() != null ? fromItem.getAlias().getName() : "";
            try {
                subquery = parse(fromItem.toString());
                subquery.setSubqueryDepth(0);
            } catch (Exception e) {
                log.debug("Failed to parse join subselect: {}", e.getMessage());
            }
            tableName = alias != null && !alias.isEmpty() ? alias : "";
            if (fromItem.getAlias() != null && fromItem.getAlias().getName() != null) {
                aliasMap.put(fromItem.getAlias().getName(), "SUBQUERY");
            }
        } else {
            // fallback: attempt to parse whatever the fromItem string is
            try {
                ParsedQuery pq = parse(fromItem.toString());
                if (pq != null) {
                    subquery = pq;
                    tableName = fromItem.getAlias() != null ? fromItem.getAlias().getName() : "";
                }
            } catch (Exception e) {
                // ignore
            }
        }

        List<String> joinColumns = new ArrayList<>();
        String condition = "";
        List<ParsedQuery.JoinKey> joinKeys = new ArrayList<>();
        if (join.getOnExpressions() != null && !join.getOnExpressions().isEmpty()) {
            Expression onExpr = join.getOnExpressions().iterator().next();
            condition = onExpr.toString();
            extractColumnsFromExpression(onExpr, joinColumns);
            extractJoinKeyPairs(onExpr, joinKeys);
        }

        return ParsedQuery.JoinInfo.builder()
                .type(joinType)
                .table(tableName)
                .alias(alias)
                .condition(condition)
                .joinColumns(joinColumns)
                .joinKeys(joinKeys)
                .subquery(subquery)
                .build();
    }

    private void extractJoinKeyPairs(Expression expr, List<ParsedQuery.JoinKey> keys) {
        if (expr == null) return;

        // Unwrap parentheses
        if (expr.getClass().getSimpleName().equals("Parenthesis")) {
            try {
                Expression inner = (Expression) expr.getClass().getMethod("getExpression").invoke(expr);
                extractJoinKeyPairs(inner, keys);
                return;
            } catch (Exception ignored) {
            }
        }

        if (expr instanceof EqualsTo eq) {
            Expression l = eq.getLeftExpression();
            Expression r = eq.getRightExpression();
            if (l instanceof Column lc && r instanceof Column rc) {
                String left = lc.getTable() != null && lc.getTable().getName() != null && !lc.getTable().getName().isEmpty()
                        ? lc.getTable().getName() + "." + lc.getColumnName()
                        : lc.getColumnName();
                String right = rc.getTable() != null && rc.getTable().getName() != null && !rc.getTable().getName().isEmpty()
                        ? rc.getTable().getName() + "." + rc.getColumnName()
                        : rc.getColumnName();
                keys.add(new ParsedQuery.JoinKey(left, right));
            }
        } else if (expr instanceof AndExpression and) {
            extractJoinKeyPairs(and.getLeftExpression(), keys);
            extractJoinKeyPairs(and.getRightExpression(), keys);
        } else if (expr instanceof OrExpression or) {
            extractJoinKeyPairs(or.getLeftExpression(), keys);
            extractJoinKeyPairs(or.getRightExpression(), keys);
        } else if (expr instanceof BinaryExpression be) {
            // try to inspect equals-like binary expressions
            if (be instanceof EqualsTo) {
                extractJoinKeyPairs((EqualsTo) be, keys);
            }
        }
    }

    private ParsedQuery.Condition parseWhereExpression(Expression expression,
            List<ParsedQuery> subqueries,
            int depth) {
        if (expression == null) {
            return null;
        }

        // Unwrap parentheses (Parenthesis is deprecated in newer versions)
        if (expression.getClass().getSimpleName().equals("Parenthesis")) {
            try {
                Expression innerExpr = (Expression) expression.getClass().getMethod("getExpression").invoke(expression);
                return parseWhereExpression(innerExpr, subqueries, depth);
            } catch (Exception e) {
                log.warn("Failed to extract inner expression from parenthesis: {}", e.getMessage());
            }
        }

        if (expression instanceof AndExpression and) {
            ParsedQuery.Condition left = parseWhereExpression(and.getLeftExpression(), subqueries, depth);
            ParsedQuery.Condition right = parseWhereExpression(and.getRightExpression(), subqueries, depth);
            return new ParsedQuery.AndCondition(left, right);

        } else if (expression instanceof OrExpression or) {
            ParsedQuery.Condition left = parseWhereExpression(or.getLeftExpression(), subqueries, depth);
            ParsedQuery.Condition right = parseWhereExpression(or.getRightExpression(), subqueries, depth);
            return new ParsedQuery.OrCondition(left, right);

        } else if (expression instanceof ComparisonOperator comp) {
            if (comp.getLeftExpression() instanceof Column col) {
                String operator = comp.getStringExpression();
                boolean isParameterized = comp.getRightExpression() instanceof JdbcParameter;
                String value = comp.getRightExpression().toString();

                // Check for subqueries in right side
                if (comp.getRightExpression() instanceof Select subSelect) {
                    ParsedQuery subquery = parse(subSelect.toString());
                    subquery.setSubqueryDepth(depth + 1);
                    subqueries.add(subquery);
                }

                return new ParsedQuery.ComparisonCondition(
                        col.getColumnName(),
                        col.getTable() != null ? col.getTable().getName() : "",
                        operator,
                        value,
                        isParameterized
                );
            }

        } else if (expression instanceof InExpression in) {
            if (in.getLeftExpression() instanceof Column col) {
                List<String> values = new ArrayList<>();
                boolean isParameterized = false;
                ParsedQuery subqueryRef = null;

                // Try to read items list via reflection (works across different jsqlparser versions)
                try {
                    java.lang.reflect.Method m = in.getClass().getMethod("getRightItemsList");
                    Object itemsList = m.invoke(in);
                    if (itemsList != null) {
                        try {
                            java.lang.reflect.Method ge = itemsList.getClass().getMethod("getExpressions");
                            Object rawList = ge.invoke(itemsList);
                            if (rawList instanceof java.util.List<?> list) {
                                for (Object o : list) {
                                    if (o instanceof Expression e) {
                                        if (e instanceof JdbcParameter) {
                                            isParameterized = true;
                                        }
                                        values.add(e.toString());
                                    } else if (o != null) {
                                        values.add(o.toString());
                                    }
                                }
                            }
                        } catch (NoSuchMethodException ignored) {
                        }
                    }
                } catch (NoSuchMethodException ignored) {
                } catch (Exception e) {
                    log.debug("IN items reflection failed: {}", e.getMessage());
                }

                // Fallback to right expression (could be Select or simple expressions)
                if (values.isEmpty()) {
                    Expression re = in.getRightExpression();
                    if (re instanceof Select subSelect) {
                        subqueryRef = parse(subSelect.toString());
                        subqueryRef.setSubqueryDepth(depth + 1);
                        subqueries.add(subqueryRef);
                    } else if (re != null) {
                        if (re instanceof JdbcParameter) {
                            isParameterized = true;
                        }
                        values.add(re.toString());
                    }
                }

                return new ParsedQuery.InCondition(
                        col.getColumnName(),
                        col.getTable() != null ? col.getTable().getName() : "",
                        values,
                        isParameterized,
                        subqueryRef
                );
            } else {
                // if left is not a column, still check for subselect on right
                if (in.getRightExpression() instanceof Select subSelect) {
                    ParsedQuery subquery = parse(subSelect.toString());
                    subquery.setSubqueryDepth(depth + 1);
                    subqueries.add(subquery);
                }
                return null;
            }

        } else if (expression instanceof Between between) {
            if (between.getLeftExpression() instanceof Column col) {
                Expression start = between.getBetweenExpressionStart();
                Expression end = between.getBetweenExpressionEnd();
                String startVal = start != null ? start.toString() : null;
                String endVal = end != null ? end.toString() : null;
                boolean isParameterized = (start instanceof JdbcParameter) || (end instanceof JdbcParameter);

                return new ParsedQuery.BetweenCondition(
                        col.getColumnName(),
                        col.getTable() != null ? col.getTable().getName() : "",
                        startVal,
                        endVal,
                        isParameterized
                );
            }

        } else if (expression instanceof LikeExpression like) {
            if (like.getLeftExpression() instanceof Column col) {
                Expression right = like.getRightExpression();
                String pattern = right != null ? right.toString() : null;
                boolean isParameterized = right instanceof JdbcParameter;

                return new ParsedQuery.LikeCondition(
                        col.getColumnName(),
                        col.getTable() != null ? col.getTable().getName() : "",
                        pattern,
                        isParameterized
                );
            }

        } else if (expression instanceof IsNullExpression isNull) {
            if (isNull.getLeftExpression() instanceof Column col) {
                return new ParsedQuery.IsNullCondition(
                        col.getColumnName(),
                        col.getTable() != null ? col.getTable().getName() : "",
                        isNull.isNot()
                );
            }

        } else if (expression instanceof ExistsExpression exists) {
            if (exists.getRightExpression() instanceof Select subSelect) {
                ParsedQuery subquery = parse(subSelect.toString());
                subquery.setSubqueryDepth(depth + 1);
                subqueries.add(subquery);
                return new ParsedQuery.ExistsCondition(subquery);
            }
        }

        return null;
    }

    private List<ParsedQuery.WhereCondition> flattenConditions(ParsedQuery.Condition condition) {
        List<ParsedQuery.WhereCondition> result = new ArrayList<>();
        flattenConditionsRecursive(condition, result);
        return result;
    }

    private void flattenConditionsRecursive(ParsedQuery.Condition condition, List<ParsedQuery.WhereCondition> result) {
        if (condition == null) {
            return;
        }

        if (condition instanceof ParsedQuery.WhereCondition whereCondition) {
            result.add(whereCondition);
        } else if (condition instanceof ParsedQuery.AndCondition andCondition) {
            flattenConditionsRecursive(andCondition.left, result);
            flattenConditionsRecursive(andCondition.right, result);
        } else if (condition instanceof ParsedQuery.OrCondition orCondition) {
            flattenConditionsRecursive(orCondition.left, result);
            flattenConditionsRecursive(orCondition.right, result);
        }
    }

    private void extractColumnsFromExpression(Expression expr, List<String> columns) {
        if (expr instanceof Column col) {
            columns.add(col.getColumnName());
        } else if (expr instanceof EqualsTo eq) {
            extractColumnsFromExpression(eq.getLeftExpression(), columns);
            extractColumnsFromExpression(eq.getRightExpression(), columns);
        } else if (expr instanceof AndExpression and) {
            extractColumnsFromExpression(and.getLeftExpression(), columns);
            extractColumnsFromExpression(and.getRightExpression(), columns);
        }
    }

    private void parseInsert(Insert insert, ParsedQuery.ParsedQueryBuilder builder) {
        List<String> tables = new ArrayList<>();
        tables.add(insert.getTable().getName());
        builder.tables(tables);

        List<String> columns = new ArrayList<>();
        if (insert.getColumns() != null) {
            insert.getColumns().forEach(col -> columns.add(col.getColumnName()));
        }
        builder.columns(columns);
    }

    private void parseUpdate(Update update, ParsedQuery.ParsedQueryBuilder builder) {
        List<String> tables = new ArrayList<>();
        tables.add(update.getTable().getName());
        builder.tables(tables);

        List<String> columns = new ArrayList<>();
        if (update.getUpdateSets() != null) {
            update.getUpdateSets().forEach(set -> {
                set.getColumns().forEach(col -> columns.add(col.getColumnName()));
            });
        }
        builder.columns(columns);

        // Parse WHERE clause
        List<ParsedQuery> subqueries = new ArrayList<>();
        ParsedQuery.Condition whereCondition = null;
        if (update.getWhere() != null) {
            whereCondition = parseWhereExpression(update.getWhere(), subqueries, 0);
        }
        if (whereCondition != null) {
            builder.whereConditions(flattenConditions(whereCondition));
        }
        builder.subqueries(subqueries);
    }

    private void parseDelete(Delete delete, ParsedQuery.ParsedQueryBuilder builder) {
        List<String> tables = new ArrayList<>();
        tables.add(delete.getTable().getName());
        builder.tables(tables);

        // Parse WHERE clause
        List<ParsedQuery> subqueries = new ArrayList<>();
        ParsedQuery.Condition whereCondition = null;
        if (delete.getWhere() != null) {
            whereCondition = parseWhereExpression(delete.getWhere(), subqueries, 0);
        }
        if (whereCondition != null) {
            builder.whereConditions(flattenConditions(whereCondition));
        }
        builder.subqueries(subqueries);
    }
}

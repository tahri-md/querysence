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
import java.util.List;

@Component
@Slf4j
public class SQLParserEngine {

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
        
        for (SelectItem<?> item : plainSelect.getSelectItems()) {
            if (item.getExpression() instanceof AllColumns) {
                columns.add("*");
            } else if (item.getExpression() instanceof AllTableColumns atc) {
                columns.add(atc.getTable().getName() + ".*");
            } else if (item.getExpression() instanceof Column col) {
                columns.add(col.getColumnName());
            } else if (item.getExpression() instanceof Function func) {
                aggregates.add(func.getName());
                columns.add(func.toString());
            } else {
                columns.add(item.toString());
            }
        }
        builder.columns(columns);
        builder.aggregateFunctions(aggregates);
        builder.hasDistinct(plainSelect.getDistinct() != null);

        // Parse FROM clause
        List<String> tables = new ArrayList<>();
        if (plainSelect.getFromItem() != null) {
            extractTables(plainSelect.getFromItem(), tables, builder);
        }
        builder.tables(tables);

        // Parse JOINs
        List<ParsedQuery.JoinInfo> joins = new ArrayList<>();
        if (plainSelect.getJoins() != null) {
            for (Join join : plainSelect.getJoins()) {
                ParsedQuery.JoinInfo joinInfo = parseJoin(join);
                joins.add(joinInfo);
                if (join.getFromItem() instanceof Table table) {
                    tables.add(table.getName());
                }
            }
        }
        builder.joins(joins);

        // Parse WHERE clause
        List<ParsedQuery.WhereCondition> conditions = new ArrayList<>();
        List<ParsedQuery> subqueries = new ArrayList<>();
        if (plainSelect.getWhere() != null) {
            parseWhereExpression(plainSelect.getWhere(), conditions, subqueries, depth);
        }
        builder.whereConditions(conditions);
        builder.subqueries(subqueries);

        // Parse GROUP BY
        if (plainSelect.getGroupBy() != null) {
            List<String> groupByCols = new ArrayList<>();
            plainSelect.getGroupBy().getGroupByExpressionList().forEach(expr -> {
                if (expr instanceof Column col) {
                    groupByCols.add(col.getColumnName());
                }
            });
            builder.groupByColumns(groupByCols);
        }

        // Parse HAVING
        builder.hasHaving(plainSelect.getHaving() != null);

        // Parse ORDER BY
        if (plainSelect.getOrderByElements() != null) {
            List<String> orderByCols = new ArrayList<>();
            plainSelect.getOrderByElements().forEach(elem -> {
                if (elem.getExpression() instanceof Column col) {
                    orderByCols.add(col.getColumnName());
                }
            });
            builder.orderByColumns(orderByCols);
        }
    }

    private void extractTables(FromItem fromItem, List<String> tables, ParsedQuery.ParsedQueryBuilder builder) {
        if (fromItem instanceof Table table) {
            tables.add(table.getName());
        } else if (fromItem instanceof Select subSelect) {
            ParsedQuery subquery = parse(subSelect.toString());
            List<ParsedQuery> subqueries = builder.build().getSubqueries();
            if (subqueries == null) subqueries = new ArrayList<>();
            subqueries.add(subquery);
            builder.subqueries(subqueries);
        }
    }

    private ParsedQuery.JoinInfo parseJoin(Join join) {
        String joinType = "INNER";
        if (join.isLeft()) joinType = "LEFT";
        else if (join.isRight()) joinType = "RIGHT";
        else if (join.isFull()) joinType = "FULL";
        else if (join.isCross()) joinType = "CROSS";

        String tableName = "";
        String alias = "";
        if (join.getFromItem() instanceof Table table) {
            tableName = table.getName();
            alias = table.getAlias() != null ? table.getAlias().getName() : "";
        }

        List<String> joinColumns = new ArrayList<>();
        String condition = "";
        if (join.getOnExpressions() != null && !join.getOnExpressions().isEmpty()) {
            Expression onExpr = join.getOnExpressions().iterator().next();
            condition = onExpr.toString();
            extractColumnsFromExpression(onExpr, joinColumns);
        }

        return ParsedQuery.JoinInfo.builder()
                .type(joinType)
                .table(tableName)
                .alias(alias)
                .condition(condition)
                .joinColumns(joinColumns)
                .build();
    }

    private void parseWhereExpression(Expression expression, 
                                       List<ParsedQuery.WhereCondition> conditions,
                                       List<ParsedQuery> subqueries,
                                       int depth) {
        if (expression instanceof AndExpression and) {
            parseWhereExpression(and.getLeftExpression(), conditions, subqueries, depth);
            parseWhereExpression(and.getRightExpression(), conditions, subqueries, depth);
        } else if (expression instanceof OrExpression or) {
            parseWhereExpression(or.getLeftExpression(), conditions, subqueries, depth);
            parseWhereExpression(or.getRightExpression(), conditions, subqueries, depth);
        } else if (expression instanceof ComparisonOperator comp) {
            ParsedQuery.WhereCondition condition = parseComparisonOperator(comp);
            if (condition != null) conditions.add(condition);
            
            // Check for subqueries
            if (comp.getRightExpression() instanceof Select subSelect) {
                ParsedQuery subquery = parse(subSelect.toString());
                subquery.setSubqueryDepth(depth + 1);
                subqueries.add(subquery);
            }
        } else if (expression instanceof InExpression in) {
            if (in.getLeftExpression() instanceof Column col) {
                conditions.add(ParsedQuery.WhereCondition.builder()
                        .column(col.getColumnName())
                        .table(col.getTable() != null ? col.getTable().getName() : "")
                        .operator("IN")
                        .build());
            }
            if (in.getRightExpression() instanceof Select subSelect) {
                ParsedQuery subquery = parse(subSelect.toString());
                subquery.setSubqueryDepth(depth + 1);
                subqueries.add(subquery);
            }
        } else if (expression instanceof Between between) {
            if (between.getLeftExpression() instanceof Column col) {
                conditions.add(ParsedQuery.WhereCondition.builder()
                        .column(col.getColumnName())
                        .table(col.getTable() != null ? col.getTable().getName() : "")
                        .operator("BETWEEN")
                        .build());
            }
        } else if (expression instanceof LikeExpression like) {
            if (like.getLeftExpression() instanceof Column col) {
                conditions.add(ParsedQuery.WhereCondition.builder()
                        .column(col.getColumnName())
                        .table(col.getTable() != null ? col.getTable().getName() : "")
                        .operator("LIKE")
                        .build());
            }
        } else if (expression instanceof IsNullExpression isNull) {
            if (isNull.getLeftExpression() instanceof Column col) {
                conditions.add(ParsedQuery.WhereCondition.builder()
                        .column(col.getColumnName())
                        .table(col.getTable() != null ? col.getTable().getName() : "")
                        .operator(isNull.isNot() ? "IS NOT NULL" : "IS NULL")
                        .build());
            }
        } else if (expression instanceof ExistsExpression exists) {
            if (exists.getRightExpression() instanceof Select subSelect) {
                ParsedQuery subquery = parse(subSelect.toString());
                subquery.setSubqueryDepth(depth + 1);
                subqueries.add(subquery);
            }
        }
    }

    private ParsedQuery.WhereCondition parseComparisonOperator(ComparisonOperator comp) {
        if (comp.getLeftExpression() instanceof Column col) {
            String operator = comp.getStringExpression();
            boolean isParameterized = comp.getRightExpression() instanceof JdbcParameter;
            String value = comp.getRightExpression().toString();

            return ParsedQuery.WhereCondition.builder()
                    .column(col.getColumnName())
                    .table(col.getTable() != null ? col.getTable().getName() : "")
                    .operator(operator)
                    .value(value)
                    .isParameterized(isParameterized)
                    .build();
        }
        return null;
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
        List<ParsedQuery.WhereCondition> conditions = new ArrayList<>();
        if (update.getWhere() != null) {
            parseWhereExpression(update.getWhere(), conditions, new ArrayList<>(), 0);
        }
        builder.whereConditions(conditions);
    }

    private void parseDelete(Delete delete, ParsedQuery.ParsedQueryBuilder builder) {
        List<String> tables = new ArrayList<>();
        tables.add(delete.getTable().getName());
        builder.tables(tables);

        // Parse WHERE clause
        List<ParsedQuery.WhereCondition> conditions = new ArrayList<>();
        if (delete.getWhere() != null) {
            parseWhereExpression(delete.getWhere(), conditions, new ArrayList<>(), 0);
        }
        builder.whereConditions(conditions);
    }
}

package persistence.query;

import entity.Column;
import entity.Entity;
import entity.Table;
import io.vavr.control.Try;
import io.vertx.sqlclient.Tuple;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author <a href="mailto:josephharveyangeles@gmail.com">Joseph Harvey Angeles - <i>@{yev}</i></a>
 * @since 9/12/20
 */
public class QueryBuilder {

    /**
     * @author <a href="mailto:josephharveyangeles@gmail.com">Joseph Harvey Angeles - <i>@{yev}</i></a>
     * @since 7/8/20
     */
    private enum QueryType {
        SELECT,
        INSERT,
        UPDATE,
        DELETE
    }

    private enum LogicOperator {
        AND,
        OR
    }

    private final Map<QueryType, Function<String, String>> buildFuncs;
    private final List<Object> values;
    private final List<String> columns;
    private final List<OperatorEntry> ops;

    private final QueryType queryType;
    private final String tableName;

    private String whereColumn;
    private String whereOp;
    private Object whereValue;


    public QueryBuilder columns(String ... columns) {
        this.columns.addAll(Arrays.asList(columns));
        return this;
    }

    public QueryBuilder allColumns() {
        this.columns.add("*");
        return this;
    }

    public QueryBuilder set(String column, Object value) {
        this.columns.add(column);
        this.values.add(value);
        return this;
    }

    public QueryBuilder property(String column, Object value) {
        this.columns.add(column);
        this.values.add(value);
        return this;
    }

    public QueryBuilder where(String column, Object value) {
        this.whereColumn = column;
        this.whereOp = "=";
        this.whereValue = value;
        return this;
    }

    public QueryBuilder where(UUID id) {
        this.whereColumn = "id";
        this.whereOp = "=";
        this.whereValue = id;
        return this;
    }

    public QueryBuilder where(String column, String op, Object value) {
        this.whereColumn = column;
        this.whereOp = op;
        this.whereValue = value;
        return this;
    }

    public QueryBuilder and(String column, Object value) {
        this.ops.add(new OperatorEntry(LogicOperator.AND, column, value));
        return this;
    }

    public QueryBuilder and(String column, String op, Object value) {
        this.ops.add(new OperatorEntry(LogicOperator.AND, column, op, value));
        return this;
    }

    public QueryBuilder or(String column, Object value) {
        this.ops.add(new OperatorEntry(LogicOperator.OR, column, value));
        return this;
    }

    public QueryBuilder or(String column, String op, Object value) {
        this.ops.add(new OperatorEntry(LogicOperator.OR, column, op, value));
        return this;
    }

    public static QueryBuilder select(String tableName) {
        return new QueryBuilder(tableName, QueryType.SELECT);
    }

    public static <T extends Entity> QueryBuilder select(Class<T> tClass) {
        return new QueryBuilder(tClass, QueryType.SELECT);
    }

    public static QueryBuilder update(String tableName) {
        return new QueryBuilder(tableName, QueryType.UPDATE);
    }

    public static <T extends Entity> QueryBuilder update(Class<T> tClass) {
        return new QueryBuilder(tClass, QueryType.UPDATE);
    }

    public static QueryBuilder insert(String tableName) {
        return new QueryBuilder(tableName, QueryType.INSERT);
    }

    public static <T extends Entity> QueryBuilder insert(Class<T> tClass) {
        return new QueryBuilder(tClass, QueryType.INSERT);
    }

    public static QueryBuilder delete(String tableName) {
        return new QueryBuilder(tableName, QueryType.DELETE);
    }

    public static <T extends Entity> QueryBuilder delete(Class<T> tClass) {
        return new QueryBuilder(tClass, QueryType.DELETE);
    }

    public Query build() {
        String query = this.buildFuncs.get(this.queryType).apply(this.tableName);
        String SQL = String.format("%s %s", query, buildWhereClause());
        this.values.add(whereValue);
        this.ops.forEach(op -> this.values.add(op.getValue()));
        return new QueryImpl(SQL, this.values);
    }

    public static <T extends Entity> Query insert(T entity) {
        return new QueryImpl(
                buildInsertSQL(toTableName(entity.getClass()), getColumns(entity.getClass())),
                toTuple(entity));
    }

    public static <T extends Entity> Query update(T entity) {
        String SQL = String.format(
                "%s WHERE id='%s'",
                buildUpdateSQL(toTableName(entity.getClass()), getColumns(entity.getClass())),
                Try.of(() -> entity.getClass()
                        .getDeclaredMethod("getId")
                        .invoke(entity))
                        .getOrNull()
        );
        return new QueryImpl(SQL, toTuple(entity));
    }

    public static <T extends Entity> Query delete(T entity) {
        Object uuid = Try.of(() -> entity.getClass()
                .getDeclaredMethod("getId")
                .invoke(entity))
                .getOrNull();
        String SQL = String.format("DELETE FROM %s WHERE id=$1", toTableName(entity.getClass()));
        return new QueryImpl(SQL, Tuple.of(uuid));
    }

    private String buildSelectSQL(String tableName) {
        String format = "%s %s FROM %s";
        String columns = StringUtils.join(this.columns, ",");
        return String.format(format, QueryType.SELECT, columns, tableName);
    }

    private String buildUpdateSQL(String tableName) {
        return buildUpdateSQL(tableName, this.columns.toArray(new String[0]));
    }

    private static String buildUpdateSQL(String tableName, String ... columns) {
        String format = "%s %s SET %s";
        String values = IntStream
                .rangeClosed(1, columns.length)
                .mapToObj(i -> String.format(
                        "%s=$%d", columns[i - 1], i))
                .collect(Collectors.joining(", "));
        return String.format(format, QueryType.UPDATE, tableName, values);
    }

    private String buildDeleteSQL(String tableName) {
        return String.format("%s from %s", QueryType.DELETE, tableName);
    }

    private String buildInsertSQL(String tableName) {
        return buildInsertSQL(tableName, this.columns.toArray(new String[0]));
    }

    private static String buildInsertSQL(String tableName, String ... columns) {
        String format = "%s INTO %s (%s) VALUES (%s)";
        return String.format(
                format,
                QueryType.INSERT,
                tableName,
                String.join(",", columns),
                buildValuesForInsert(columns.length));
    }

    private static String buildValuesForInsert(int size) {
        return IntStream
                .rangeClosed(1, size)
                .mapToObj(i -> String.format("$%d", i))
                .collect(Collectors.joining(", "));
    }

    private String buildWhereClause() {
        if (this.whereColumn == null) {
            return StringUtils.EMPTY;
        }
        int startIndex = this.values.size() + 1;
        String logicals = IntStream
                .rangeClosed(1, this.ops.size())
                .mapToObj(i -> {
                    OperatorEntry e = this.ops.get(i - 1);
                    return String.format(
                            "%s %s%s$%d",
                            e.getLogicOp(),
                            e.getColumn(),
                            e.getOperator(),
                            i + startIndex);
                })
                .collect(Collectors.joining(" "));
        return String.format("WHERE %s%s$%d %s", this.whereColumn, this.whereOp, startIndex, logicals).trim();
    }

    private static <T extends Entity> String toTableName(Class<T> clasz) {
        return clasz.getAnnotation(Table.class).value();
    }

    private static <T extends Entity> Tuple toTuple(T entity) {
        return Tuple.tuple(Arrays
                .stream(entity.getClass().getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Column.class))
                .map(field -> {
                    final String prefix = field.getType().equals(boolean.class) ? "is" : "get";
                    return Try.of(() -> entity.getClass()
                            .getDeclaredMethod(prefix + StringUtils.capitalize(field.getName()))
                            .invoke(entity))
                            .getOrNull();
                })
                .collect(Collectors.toList()));
    }

    private static <T extends Entity> String[] getColumns(Class<T> clasz) {
        return Arrays
                .stream(clasz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Column.class))
                .map(field -> field.getDeclaredAnnotation(Column.class).value())
                .toArray(String[]::new);
    }

    QueryBuilder(String tableName, QueryType queryType) {
        this.tableName = tableName;
        this.queryType = queryType;
        this.values = new ArrayList<>();
        this.columns = new ArrayList<>();
        this.ops = new ArrayList<>();
        this.buildFuncs = new HashMap<>();
        this.buildFuncs.put(QueryType.SELECT, this::buildSelectSQL);
        this.buildFuncs.put(QueryType.UPDATE, this::buildUpdateSQL);
        this.buildFuncs.put(QueryType.DELETE, this::buildDeleteSQL);
        this.buildFuncs.put(QueryType.INSERT, this::buildInsertSQL);
    }

    <T extends Entity> QueryBuilder(Class<T> tClass, QueryType queryType) {
        this(tClass.getAnnotation(Table.class).value(), queryType);
    }

    final static class OperatorEntry {
        private final LogicOperator logicOp;
        private final String column;
        private final String op;
        private final Object value;

        OperatorEntry (LogicOperator logicOp, String column, Object value) {
            this.logicOp = logicOp;
            this.column = column;
            this.value = value;
            this.op = "=";
        }

        OperatorEntry (LogicOperator logicOp, String column, String op, Object value) {
            this.logicOp = logicOp;
            this.column = column;
            this.op = op;
            this.value = value;
        }

        LogicOperator getLogicOp() { return this.logicOp; }
        String getColumn() { return this.column; }
        String getOperator() { return this.op; }
        Object getValue() { return this.value; }

    }

    static final class QueryImpl implements Query {
        private final String sql;
        private final Tuple tuple;

        QueryImpl(String sql, Tuple tuple) {
            this.sql = sql;
            this.tuple = tuple;
        }

        QueryImpl(String sql, List<Object> values) {
            this(sql, Tuple.tuple(values));
        }

        @Override
        public String sql() {
            return sql;
        }

        @Override
        public Tuple tuple() {
            return tuple;
        }
    }
}

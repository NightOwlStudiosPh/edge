package ph.com.nightowlstudios.persistence.query;

import org.apache.commons.lang3.StringUtils;
import ph.com.nightowlstudios.entity.Entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author <a href="mailto:josephharveyangeles@gmail.com">Joseph Harvey Angeles - <i>yev</i></a>
 * @since 5/14/21
 **/
public class JoinSelectQueryBuilder {

  private final String tableName;
  private List<String> columns;

  private String whereColumn;
  private String whereOp;
  private Object whereValue;

  private List<String> logicals;

  <T extends Entity> JoinSelectQueryBuilder(Class<T> fromTable) {
    this(Entity.getTableName(fromTable));
  }

  JoinSelectQueryBuilder(String fromTable) {
    this.tableName = fromTable;
  }

  public JoinSelectQueryBuilder column(String column) {
    if (this.columns == null) {
      this.columns = new ArrayList<>();
    }
    this.columns.add(column);
    return this;
  }

  public JoinSelectQueryBuilder columns(String... columns) {
    if (this.columns == null) {
      this.columns = new ArrayList<>();
    }
    this.columns.addAll(Arrays.asList(columns));
    return this;
  }

  public JoinSelectQueryBuilder where(String column, Object value) {
    this.whereColumn = column;
    this.whereOp = "=";
    this.whereValue = value.toString();
    return this;
  }

  public JoinSelectQueryBuilder where(String column, String op, Object value) {
    this.whereColumn = column;
    this.whereOp = op;
    this.whereValue = value.toString();
    return this;
  }

  public JoinSelectQueryBuilder and(String column, Object value) {
    return and(column, "=", value);
  }

  public JoinSelectQueryBuilder and(String column, String op, Object value) {
    return addLogical("AND", column, op, value);
  }

  public JoinSelectQueryBuilder or(String column, Object value) {
    return or(column, "=", value);
  }

  public JoinSelectQueryBuilder or(String column, String op, Object value) {
    return addLogical("OR", column, op, value);
  }

  private JoinSelectQueryBuilder addLogical(String logicOp, String column, String op, Object value) {
    if (logicals == null) {
      logicals = new ArrayList<>();
    }
    logicals.add(String.format(
      "%s %s %s '%s'",
      logicOp, column, op, value.toString()).trim());
    return this;
  }

  String build() {
    StringBuilder query = new StringBuilder();
    query.append("SELECT ")
      .append(StringUtils.join(this.columns, QueryBuilder.COMMA))
      .append(String.format(" FROM %s", tableName));


    if (StringUtils.isNotEmpty(whereColumn)) {
      query.append(String.format(" WHERE %s %s '%s'", whereColumn, whereOp, whereValue));
      if (logicals != null) {
        this.logicals.forEach(line -> query
          .append(QueryBuilder.WHITESPACE)
          .append(line)
          .append(QueryBuilder.WHITESPACE)
        );
      }
    }

    String[] groupColumns = this.columns
      .stream()                 // include only plain column
      .filter(column -> !column.contains("("))
      .map(column -> {
        String[] tokens = column.split(QueryBuilder.WHITESPACE);
        if (tokens.length >= 1) {
          return tokens[0].trim();
        }
        return column.trim();
      })
      .toArray(String[]::new);

    return query
      .append(" GROUP BY ")
      .append(StringUtils.join(groupColumns, QueryBuilder.COMMA))
      .toString().trim();
  }

}

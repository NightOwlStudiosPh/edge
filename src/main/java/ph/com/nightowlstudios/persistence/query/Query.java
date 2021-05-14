package ph.com.nightowlstudios.persistence.query;

import io.vertx.sqlclient.Tuple;
import ph.com.nightowlstudios.entity.Entity;

import java.util.UUID;

/**
 * @author <a href="mailto:josephharveyangeles@gmail.com">Joseph Harvey Angeles - <i>@yev</i></a>
 * @since 9/12/20
 */
public interface Query {
  static QueryBuilder select(String fromTable) {
    return QueryBuilder.select(fromTable);
  }

  static <T extends Entity> QueryBuilder select(Class<T> entityClass) {
    return QueryBuilder.select(entityClass);
  }

  static QueryBuilder update(String table) {
    return QueryBuilder.update(table);
  }

  static <T extends Entity> QueryBuilder update(Class<T> entityClass) {
    return QueryBuilder.update(entityClass);
  }

  static QueryBuilder insert(String into) {
    return QueryBuilder.insert(into);
  }

  static <T extends Entity> QueryBuilder insert(Class<T> entityClass) {
    return QueryBuilder.insert(entityClass);
  }

  static QueryBuilder delete(String from) {
    return QueryBuilder.delete(from);
  }

  static <T extends Entity> QueryBuilder delete(Class<T> from) {
    return QueryBuilder.delete(from);
  }

  static Query select(String tableName, UUID id) {
    return Query.select(tableName).allColumns().where(id).build();
  }

  static <T extends Entity> Query select(Class<T> entityClass, UUID id) {
    return Query.select(entityClass).allColumns().where(id).build();
  }

  static <T extends Entity> Query insert(T entity) {
    return QueryBuilder.insert(entity);
  }

  static <T extends Entity> Query update(T entity) {
    return QueryBuilder.update(entity);
  }

  static <T extends Entity> Query delete(T entity) {
    return QueryBuilder.delete(entity);
  }

  static <T extends Entity> JoinSelectQueryBuilder joinSelect(Class<T> entityClass) { return new JoinSelectQueryBuilder(entityClass); }

  static JoinSelectQueryBuilder joinSelect(String tableName) { return new JoinSelectQueryBuilder(tableName); }

  String sql();

  Tuple tuple();
}


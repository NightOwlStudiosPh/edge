package ph.com.nightowlstudios.repository;

import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
import ph.com.nightowlstudios.dto.DTO;
import ph.com.nightowlstudios.entity.Entity;
import ph.com.nightowlstudios.persistence.Collectors;
import ph.com.nightowlstudios.persistence.PersistenceClient;
import ph.com.nightowlstudios.persistence.query.Query;
import ph.com.nightowlstudios.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collector;

/**
 * @author <a href="mailto:josephharveyangeles@gmail.com">Joseph Harvey Angeles - <i>@yev</i></a>
 * @since 7/4/20
 */
public abstract class Repository {

  private final PersistenceClient dbClient;

  public Repository() {
    this.dbClient = new PersistenceClient();
  }

  public Repository(PersistenceClient dbClient) {
    this.dbClient = dbClient;
  }

  protected PersistenceClient db() {
    return this.dbClient;
  }

  public <T extends Entity> Future<Optional<T>> findOneById(Class<T> entityClass, UUID id) {
    return findOneById(entityClass, id, Collectors.ofEntities(entityClass));
  }

  public <T extends Entity> Future<Optional<T>> findOneById(Class<T> entityClass, String id) {
    return findOneById(entityClass, UUID.fromString(id), Collectors.ofEntities(entityClass));
  }

  public <T extends Entity, R> Future<Optional<R>> findOneById(Class<T> entityClass, UUID id, Function<Row, R> rowMapper) {
    return findOne(Query.select(entityClass, id), rowMapper);
  }

  public <T extends Entity, R> Future<Optional<R>> findOneById(Class<T> entityClass, String id, Function<Row, R> rowMapper) {
    return findOneById(entityClass, UUID.fromString(id), rowMapper);
  }

  public <T extends Entity> Future<Optional<T>> findOneById(Class<T> entityClass, UUID id, Collector<Row, ?, List<T>> collector) {
    return db()
      .query(
        Query.select(entityClass, id),
        collector
      ).map(Utils::getFirstElement);
  }

  public <T extends Entity> Future<List<T>> findMany(Query query, Collector<Row, ?, List<T>> collector) {
    return db().query(query, collector);
  }

  public <T> Future<List<T>> findMany(Query query, Function<Row, T> rowMapper) {
    return db().query(query, collect(rowMapper));
  }

  protected <T> Future<Optional<T>> findOne(Query q, Function<Row, T> rowMapper) {
    return db().query(q, collect(rowMapper)).map(Utils::getFirstElement);
  }

  protected <T> Collector<Row, ?, List<T>> collect(Function<Row, T> rowMapper) {
    return Collector.of(
      ArrayList::new,
      (list, row) -> list.add(rowMapper.apply(row)),
      (first, second) -> {
        first.addAll(second);
        return first;
      }
    );
  }
}

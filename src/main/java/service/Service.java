package service;

import entity.Entity;
import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
import persistence.PersistenceClient;
import persistence.query.Query;
import utils.Utils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collector;

/**
 * @author <a href="mailto:josephharveyangeles@gmail.com">Joseph Harvey Angeles - <i>@{yev}</i></a>
 * @since 7/4/20
 */
public abstract class Service {

    private final PersistenceClient dbClient;

    public Service() {
        this.dbClient = new PersistenceClient();
    }

    public Service(PersistenceClient dbClient) {
        this.dbClient = dbClient;
    }

    protected PersistenceClient db() {
        return this.dbClient;
    }

    public <T extends Entity> Future<Optional<T>> findOneById(Class<T> entityClass, UUID id, Collector<Row, ?, List<T>> collector) {
        return db()
                .query(
                        Query.select(entityClass, id),
                        collector
                ).map(Utils::getFirstElement);
    }

    public <T extends Entity> Future<Optional<T>> findOneById(Class<T> entityClass, String id, Collector<Row, ?, List<T>> collector) {
        return findOneById(entityClass, UUID.fromString(id), collector);
    }
}

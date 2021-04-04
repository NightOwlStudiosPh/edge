package ph.com.nightowlstudios.repository;

import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
import ph.com.nightowlstudios.entity.Entity;
import ph.com.nightowlstudios.persistence.PersistenceClient;
import ph.com.nightowlstudios.persistence.query.Query;
import ph.com.nightowlstudios.utils.Utils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
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

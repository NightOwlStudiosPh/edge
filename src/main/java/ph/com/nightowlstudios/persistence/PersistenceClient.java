package ph.com.nightowlstudios.persistence;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ph.com.nightowlstudios.persistence.query.Query;

import java.util.stream.Collector;

/**
 * @author <a href="mailto:josephharveyangeles@gmail.com">Joseph Harvey Angeles - <i>@yev</i></a>
 * @since 9/12/20
 */
public class PersistenceClient {

  private static final Logger log = LoggerFactory.getLogger(PersistenceClient.class);

  private final Pool pool;

  public PersistenceClient() {
    this(
      Vertx.currentContext().owner(),
      Vertx.currentContext().config().getJsonObject("db")
    );
  }

  public PersistenceClient(Vertx vertx, JsonObject dbConf) {
    PgConnectOptions connectOptions = new PgConnectOptions()
      .setDatabase(dbConf.getString("name"))
      .setPort(dbConf.getInteger("port"))
      .setHost(dbConf.getString("host"))
      .setUser(dbConf.getString("user"))
      .setPassword(dbConf.getString("password"));
    PoolOptions poolOptions = new PoolOptions().setMaxSize(dbConf.getInteger("maxPoolSize"));
    this.pool = PgPool.pool(vertx, connectOptions, poolOptions);
  }

  protected Pool pool() {
    return this.pool;
  }

  public <T> Future<T> query(Query q, Collector<Row, ?, T> collector) {
    Promise<T> promise = Promise.promise();
    log.debug("Executing SQL: {}", q.sql());
    log.debug("Against Tuples: {}", q.tuple().deepToString());
    pool()
      .preparedQuery(q.sql())
      .collecting(collector)
      .execute(q.tuple(), ar -> {
        if (ar.failed()) {
          log.error("SQL query FAIL: {}", ar.cause().getMessage());
          promise.fail(ar.cause());
          return;
        }
        promise.complete(ar.result().value());
      });
    return promise.future();
  }

  public Future<RowSet<Row>> query(Query q) {
    Promise<RowSet<Row>> promise = Promise.promise();
    log.debug("Executing SQL: {}", q.sql());
    log.debug("Against Tuples: {}", q.tuple().deepToString());
    pool()
      .preparedQuery(q.sql())
      .execute(q.tuple(), ar -> {
        if (ar.failed()) {
          log.error("SQL query FAIL: {}", ar.cause().getMessage());
          promise.fail(ar.cause());
          return;
        }
        promise.complete(ar.result());
      });
    return promise.future();
  }

  public <T> Future<T> query(String sql, Collector<Row, ?, T> collector) {
    Promise<T> promise = Promise.promise();
    log.debug("Executing plain SQL: {}", sql);
    pool()
      .query(sql)
      .collecting(collector)
      .execute(ar -> {
        if (ar.failed()) {
          log.error("SQL query FAIL: {}", ar.cause().getMessage());
          promise.fail(ar.cause());
          return;
        }
        promise.complete(ar.result().value());
      });
    return promise.future();
  }

  public Future<Void> query(String sql) {
    Promise<Void> promise = Promise.promise();
    log.debug("Executing plain SQL: {}", sql);
    pool()
      .query(sql)
      .execute(ar -> {
        if (ar.failed()) {
          log.error("SQL query FAIL: {}", ar.cause().getMessage());
          promise.fail(ar.cause());
          return;
        }
        promise.complete();
      });
    return promise.future();
  }

}


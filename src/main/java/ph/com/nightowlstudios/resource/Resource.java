package ph.com.nightowlstudios.resource;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ph.com.nightowlstudios.auth.BasicAuthentication;
import ph.com.nightowlstudios.auth.UserRole;
import ph.com.nightowlstudios.dto.DTO;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * @author <a href="mailto:josephharveyangeles@gmail.com">Joseph Harvey Angeles - <i>@yev</i></a>
 * @since 9/12/20
 */
public abstract class Resource {
  private final Logger logger;

  private final Router router;

  public Resource(Router router) {
    this.router = router;

    this.logger = LoggerFactory.getLogger(this.getClass());

    logger().info("Loading {}...", this.getClass().getCanonicalName());
    init();
    setUp();
    routes();
  }

  /**
   * For <code>Resource</code> subclasses. Use when initializing
   * and setting up new Resource variants.
   */
  void init() {
  }

  /**
   * Executed before routes declaration. Setup services and any resource needed by
   * route handlers inside this hook.
   */
  protected void setUp() {
  }

  /**
   * Define routes and their route handlers.
   */
  protected abstract void routes();

  protected Logger logger() {
    return this.logger;
  }

  protected Router router() {
    return this.router;
  }

  protected String getPathParam(RoutingContext ctx, String param) {
    return getPathParam(ctx, param, StringUtils.EMPTY);
  }

  protected String getPathParam(RoutingContext ctx, String param, String def) {
    return Optional.ofNullable(ctx.pathParam(param)).orElse(def);
  }

  protected String getRequestParam(RoutingContext ctx, String param) {
    return getRequestParam(ctx, param, StringUtils.EMPTY);
  }

  protected String getRequestParam(RoutingContext ctx, String param, String def) {
    return Optional.ofNullable(ctx.request().getParam(param)).orElse(def);
  }

  protected <T> T getAuthClaim(RoutingContext ctx, String key) {
    return ctx.user().<T>get(key);
  }

  protected <T> T getAuthClaim(RoutingContext ctx, String key, T def) {
    return Optional.ofNullable(ctx.user().<T>get(key)).orElse(def);
  }

  protected Route route(HttpMethod method, String path) {
    return router().route(method, path);
  }

  protected Route protectedRoute(HttpMethod method, String path) {
    return route(method, path).handler(BasicAuthentication.getInstance().createAuthNHandler());
  }

  protected Route protectedRoute(HttpMethod method, String path, UserRole role) {
    return router.route(method, path)
      .handler(BasicAuthentication.getInstance().createAuthNHandler())
      .handler(BasicAuthentication.getInstance().createAuthZHandler(role));
  }

  protected Route get(String path) {
    return protectedRoute(HttpMethod.GET, path);
  }

  protected Route get(String path, UserRole role) {
    return protectedRoute(HttpMethod.GET, path, role);
  }

  protected Route put(String path) {
    return protectedRoute(HttpMethod.PUT, path);
  }

  protected Route put(String path, UserRole role) {
    return protectedRoute(HttpMethod.PUT, path, role);
  }

  protected Route post(String path) {
    return protectedRoute(HttpMethod.POST, path);
  }

  protected Route post(String path, UserRole role) {
    return protectedRoute(HttpMethod.POST, path, role);
  }

  protected Route delete(String path) {
    return protectedRoute(HttpMethod.DELETE, path);
  }

  protected Route delete(String path, UserRole role) {
    return protectedRoute(HttpMethod.DELETE, path, role);
  }

  protected void endContext(RoutingContext ctx, HttpResponseStatus status, String message) {
    ctx.response().setStatusCode(status.code()).end(message);
  }

  protected void endContext(RoutingContext ctx, HttpResponseStatus status) {
    ctx.response().setStatusCode(status.code()).end(status.reasonPhrase());
  }

  protected void endContext(RoutingContext ctx, Object body) {
    ctx.response().setStatusCode(HttpResponseStatus.OK.code()).end(JsonObject.mapFrom(body).encode());
  }

  protected void endContext(RoutingContext ctx, JsonObject body) {
    ctx.response().setStatusCode(HttpResponseStatus.OK.code()).end(body.encode());
  }

  protected void endContext(RoutingContext ctx, JsonArray body) {
    ctx.response().setStatusCode(HttpResponseStatus.OK.code()).end(body.encode());
  }

  protected <T extends DTO> void endContext(RoutingContext ctx, List<T> list) {
    JsonArray body = new JsonArray();
    list.forEach(dto -> body.add(dto.toJson()));
    endContext(ctx, body);
  }

  protected <T> void on(RoutingContext ctx, Future<T> action, Handler<T> onSuccess) {
    action.onSuccess(onSuccess).onFailure(error -> this.failureHandler(ctx, error));
  }

  protected <T> Future<T> on(RoutingContext ctx, Future<T> action) {
    return action.onFailure(error -> this.failureHandler(ctx, error));
  }

  protected void failureHandler(RoutingContext ctx, Throwable cause) {
    if (cause instanceof IllegalArgumentException) {
      ctx.fail(HttpResponseStatus.BAD_REQUEST.code(), cause);
      return;
    }

    if (cause instanceof NullPointerException) {
      ctx.fail(HttpResponseStatus.NOT_FOUND.code(), cause);
      return;
    }

    if (cause instanceof IllegalAccessException ||
      cause instanceof IllegalAccessError ||
      cause instanceof SecurityException) {
      ctx.fail(HttpResponseStatus.FORBIDDEN.code(), cause);
      return;
    }

    ctx.fail(cause.getCause());
  }

  /**
   * Applies a Future <code>execute</code> function on each element of <code>list</code>.
   * The <code>Future</code> completes until all of the functions are executed.
   *
   * @param list the list to apply an <code>execute</code> function from.
   * @param execute the function to apply on each element
   * @param <T> the type of elements that <code>list</code> holds
   * @param <U> the resulting type after <code>execute</code> has been applied
   * @return the same <code>list</code> for fluent use.
   */
  protected  <T, U> Future<List<T>> onEach(List<T> list, Function<T, Future<U>> execute) {
    List<Future> futures = new ArrayList<>();
    list.forEach(item -> futures.add(execute.apply(item)));

    Promise<List<T>> promise = Promise.promise();
    CompositeFuture
      .all(futures)
      .onSuccess(ignore -> promise.complete(list))
      .onFailure(promise::fail);

    return promise.future();
  }

}


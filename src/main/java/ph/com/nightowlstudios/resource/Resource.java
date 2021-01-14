package ph.com.nightowlstudios.resource;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ph.com.nightowlstudios.auth.BasicAuthentication;
import ph.com.nightowlstudios.auth.UserRole;

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
    void init() {}

    /**
     * Executed before routes declaration. Setup services and any resource needed by
     * route handlers inside this hook.
     */
    protected abstract void setUp();

    /**
     * Define routes and their route handlers.
     */
    protected abstract void routes();

    protected Logger logger() {
        return this.logger;
    }

    protected Router router() { return this.router; }

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

    protected <T> void on(Future<T> action, Handler<T> onSuccess, RoutingContext ctx) {
        action.onSuccess(onSuccess).onFailure(error -> ctx.fail(error.getCause()));
    }

    protected <T> Future<T> on(Future<T> action, RoutingContext ctx) {
        return action.onFailure(e -> ctx.fail(e.getCause()));
    }

}


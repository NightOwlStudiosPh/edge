import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PfxOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.ErrorHandler;
import io.vertx.ext.web.handler.LoggerHandler;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:josephharveyangeles@gmail.com">Joseph Harvey Angeles - <i>@{yev}</i></a>
 * @since 7/14/20
 */
public abstract class MainVerticle extends AbstractVerticle {

    @Override
    public void start(Promise<Void> startPromise) {
        Router rootRouter = Router.router(vertx);
        rootRouter.route().handler(
                CorsHandler
                        .create(getAllowedOrigins())
                        .allowCredentials(true)
                        .allowedMethods(ALLOWED_METHODS)
                        .allowedHeaders(ALLOWED_HEADERS)
                        .exposedHeaders(EXPOSED_HEADERS)
        );

        rootRouter.route().handler(BodyHandler.create());

        rootRouter.mountSubRouter(getApiPrefix(), buildApiRouter());

        createHttpServer()
                .requestHandler(rootRouter)
                .listen(getPort(), http -> {
                    if (http.succeeded()) {
                        startPromise.complete();
                        log.info(getBannerText());
                        log.info("{} online at PORT: {}", getName(), http.result().actualPort());
                        return;
                    }
                    log.error("Error running server: " + http.cause());
                    startPromise.fail(http.cause());
                });
    }

    protected Handler<RoutingContext> createRouteLogHandler() {
        return LoggerHandler.create();
    }
    protected Handler<RoutingContext> createRouteFailureHandler() {
        return ErrorHandler.create();
    }

    protected abstract String getName();
    protected abstract int getPort();
    protected abstract String getAllowedOrigins();
    protected abstract String getApiPrefix();
    protected abstract String getBannerText();
    protected abstract boolean isProduction();
    protected abstract JsonObject getSSLConfig();
    protected abstract JWTAuth createAuthProvider();
    protected abstract Class<?>[] getResourceClasses();

    private HttpServer createHttpServer() {
        if (isProduction()) {
            return vertx.createHttpServer(createSSLHttpOptions());
        }
        return vertx.createHttpServer();
    }

    private HttpServerOptions createSSLHttpOptions () {
        JsonObject ssl = config().getJsonObject("ssl");

        HttpServerOptions options = new HttpServerOptions()
                .setUseAlpn(false)
                .setSsl(true);

        switch (ssl.getString("type", StringUtils.EMPTY)) {
            case "jks":
                options.setKeyStoreOptions(
                        new JksOptions()
                                .setPath(ssl.getString("path"))
                                .setPassword(ssl.getString("password"))
                );
                break;
            case "pfx":
                options.setPfxKeyCertOptions(
                        new PfxOptions()
                                .setPath(ssl.getString("path"))
                                .setPassword(ssl.getString("password"))
                );
                break;
            case "pem":
                options.setPemKeyCertOptions(
                        new PemKeyCertOptions()
                                .setKeyPath(ssl.getString("keyPath"))
                                .setCertPath(ssl.getString("certPath"))
                );
            default:
                log.error("Missing SSL Configuration");
                throw new RuntimeException("No SSL Configuration Provided");
        }
        return options;
    }

    private Router buildApiRouter() throws RuntimeException {
        Router router = Router.router(vertx);
        router.route().handler(createRouteLogHandler());
        router.route().failureHandler(createRouteFailureHandler());

        JWTAuth authProvider = createAuthProvider();
        for (Class<?> controller : getResourceClasses()) {
            try {
                controller
                        .getDeclaredConstructor(Router.class, JWTAuth.class)
                        .newInstance(router, authProvider);
            } catch (Exception e) {
                log.error("Unable to load {}. Error on {}. ", controller.getCanonicalName(), e.getMessage());
                e.printStackTrace();
            }
        }
        return router;
    }

    private static final Logger log = LoggerFactory.getLogger(MainVerticle.class);

    private static final Set<HttpMethod> ALLOWED_METHODS = new HashSet<>(
            Arrays.asList(
                    HttpMethod.GET,
                    HttpMethod.PUT,
                    HttpMethod.POST,
                    HttpMethod.PATCH,
                    HttpMethod.DELETE,
                    HttpMethod.OPTIONS
            )
    );

    private static final Set<String> ALLOWED_HEADERS = new HashSet<>(
            Arrays.asList(
                    HttpHeaders.CONTENT_TYPE.toString(),
                    HttpHeaders.AUTHORIZATION.toString(),
                    HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN.toString(),
                    HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS.toString(),
                    HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD.toString(),
                    HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS.toString()
            )
    );

    private static final Set<String> EXPOSED_HEADERS = new HashSet<>(Collections.singletonList(HttpHeaders.AUTHORIZATION.toString()));
}

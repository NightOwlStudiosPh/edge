package ph.com.nightowlstudios;

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
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.*;
import io.vertx.ext.web.handler.sockjs.SockJSBridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ph.com.nightowlstudios.resource.Resource;

import java.util.*;
import java.util.function.Predicate;

/**
 * @author <a href="mailto:josephharveyangeles@gmail.com">Joseph Harvey Angeles - <i>@yev</i></a>
 * @since 7/14/20
 */
public abstract class ApplicationVerticle extends AbstractVerticle {

    @Override
    public void start(Promise<Void> startPromise) {
        Router rootRouter = Router.router(vertx);
        beforeRouterCreate(rootRouter);
        rootRouter.route().handler(
                CorsHandler
                        .create(allowedOrigins())
                        .allowCredentials(allowCORSCredentials())
                        .allowedMethods(allowedMethods())
                        .allowedHeaders(allowedHeaders())
                        .exposedHeaders(exposedHeaders())
        );
        rootRouter.route().handler(BodyHandler.create());
        rootRouter.mountSubRouter(apiPrefix(), createApiRouter());
        rootRouter.mountSubRouter(webSocketPrefix(), createWebSocketRouter());

        createHttpServer()
                .requestHandler(rootRouter)
                .listen(getPort(), http -> {
                    if (http.succeeded()) {
                        startPromise.complete();
                        log.info(bannerText());
                        log.info("{} online at PORT: {}", appName(), http.result().actualPort());
                        onStart(http.result());
                        return;
                    }
                    log.error("Error running server: " + http.cause());
                    startPromise.fail(http.cause());
                    onStartFail(http.cause());
                });
    }

    /**
     * Called immediately after Router creation.
     *
     * @param router the root router
     */
    protected void beforeRouterCreate(Router router) {}

    /**
     * Called before mounting API subrouter.
     *
     * @param apiRouter api subrouter
     */
    protected void setupRoutes(Router apiRouter) {}
    protected void onStart(HttpServer httpServer) {}
    protected void onStartFail(Throwable error) {}

    protected int getPort() { return config().getInteger("port", 8888); }

    protected String appName() { return config().getString("name", "edge.api"); }
    protected String version() { return config().getString("version", "1.0"); }
    protected boolean allowCORSCredentials() { return true; }
    protected Set<HttpMethod> allowedMethods() { return  ALLOWED_METHODS; }
    protected Set<String> allowedHeaders() { return ALLOWED_HEADERS; }
    protected Set<String> exposedHeaders() { return EXPOSED_HEADERS; }
    protected String allowedOrigins() { return config().getString("allowedOrigins", "*"); }
    protected String apiPrefix() { return config().getString("apiPrefix", "/api"); }
    protected String webSocketPrefix() { return config().getString("wsPrefix", "/ws"); }

    protected String bannerText() { return BANNER_TXT; }

    protected boolean isProduction() {
        Predicate<String> isProdEnv = value ->
                value.equalsIgnoreCase("prod") ||
                value.equalsIgnoreCase("production");

        if (StringUtils.isEmpty(config().getString("env", StringUtils.EMPTY))) {
            return isProdEnv.test(Optional.ofNullable(System.getenv("env")).orElse("prod"));
        }
        return isProdEnv.test(config().getString("env"));
    }

    protected JsonObject sslConfig() {
        return config().getJsonObject("ssl", new JsonObject());
    }

    protected Handler<RoutingContext> createRouteLogHandler() {
        return LoggerHandler.create();
    }
    protected Handler<RoutingContext> createRouteFailureHandler() {
        return ErrorHandler.create(getVertx());
    }

    protected abstract  <R extends Resource> Class<R>[] getResourceClasses();

    private HttpServer createHttpServer() {
        if (isProduction() && !sslConfig().isEmpty()) {
            return vertx.createHttpServer(createSSLHttpOptions());
        }
        return vertx.createHttpServer();
    }

    private HttpServerOptions createSSLHttpOptions () {
        JsonObject ssl = sslConfig();

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

    private Router createApiRouter() throws RuntimeException {
        Router router = Router.router(vertx);
        router.route().handler(createRouteLogHandler());
        router.route().failureHandler(createRouteFailureHandler());

        for (Class<Resource> controller : getResourceClasses()) {
            try {
               controller.getDeclaredConstructor(Router.class).newInstance(router);
            } catch (Exception e) {
                log.error("Unable to load {}. Error on {}. ", controller.getCanonicalName(), e.getMessage());
                e.printStackTrace();
            }
        }
        setupRoutes(router);
        return router;
    }

    private Router createWebSocketRouter() throws RuntimeException {
        SockJSHandler handler = SockJSHandler.create(vertx);
        SockJSBridgeOptions options = new SockJSBridgeOptions();

        List<PermittedOptions> inbound = new ArrayList<>(addInboundSocketRules());
        options.setInboundPermitteds(inbound);

        List<PermittedOptions> outbound = new ArrayList<>(addOutboundSocketRules());
        options.setOutboundPermitteds(outbound);
        return  handler.bridge(options);
    }

    protected List<PermittedOptions> addInboundSocketRules() {
        return new ArrayList<>();
    }

    protected List<PermittedOptions> addOutboundSocketRules() {
        return new ArrayList<>();
    }

    private static final Logger log = LoggerFactory.getLogger(ApplicationVerticle.class);

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

    static final String BANNER_TXT = "\n" +
            "▓█████ ▓█████▄   ▄████ ▓█████    \n" +
            "▓█   ▀ ▒██▀ ██▌ ██▒ ▀█▒▓█   ▀    \n" +
            "▒███   ░██   █▌▒██░▄▄▄░▒███      \n" +
            "▒▓█  ▄ ░▓█▄   ▌░▓█  ██▓▒▓█  ▄    \n" +
            "░▒████▒░▒████▓ ░▒▓███▀▒░▒████▒   \n" +
            "░░ ▒░ ░ ▒▒▓  ▒  ░▒   ▒ ░░ ▒░ ░   \n" +
            " ░ ░  ░ ░ ▒  ▒   ░   ░  ░ ░  ░   \n" +
            "   ░    ░ ░  ░ ░ ░   ░    ░      \n" +
            "   ░  ░   ░          ░    ░  ░   \n" +
            "        ░                        \n";
}

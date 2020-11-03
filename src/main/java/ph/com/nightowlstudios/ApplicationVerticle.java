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
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.*;
import io.vertx.ext.web.handler.sockjs.SockJSBridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author <a href="mailto:josephharveyangeles@gmail.com">Joseph Harvey Angeles - <i>@yev</i></a>
 * @since 7/14/20
 */
public abstract class ApplicationVerticle<T extends ApplicationConfig> extends AbstractVerticle {

    private final T appConfig;
    protected  JWTAuth authProvider;

    protected ApplicationVerticle (T appConfig) {
        this.appConfig = appConfig;
    }

    @Override
    public void start(Promise<Void> startPromise) {
        Router rootRouter = Router.router(vertx);
        onRootRouterCreate(rootRouter);
        rootRouter.route().handler(
                CorsHandler
                        .create(appConfig.getAllowedOrigins())
                        .allowCredentials(allowCORSCredentials())
                        .allowedMethods(getAllowedMethods())
                        .allowedHeaders(getAllowedHeaders())
                        .exposedHeaders(getExposedHeaders())
        );
        rootRouter.route().handler(BodyHandler.create());
        this.authProvider = createAuthProvider();
        rootRouter.mountSubRouter(appConfig.getApiPrefix(), buildApiRouter(authProvider));
        rootRouter.mountSubRouter(appConfig.getWebSocketPrefix(), buildWebSocketRouter(authProvider));

        createHttpServer()
                .requestHandler(rootRouter)
                .listen(appConfig.getPort(), http -> {
                    if (http.succeeded()) {
                        startPromise.complete();
                        log.info(appConfig.getBannerText());
                        log.info("{} online at PORT: {}", appConfig.getApplicationName(), http.result().actualPort());
                        onStart(http.result());
                        return;
                    }
                    log.error("Error running server: " + http.cause());
                    startPromise.fail(http.cause());
                    onServerDeployFail(http.cause());
                });
    }

    protected T getAppConfig() { return this.appConfig; }
    protected JWTAuth getAuthProvider() { return this.authProvider; }

    /**
     * Called immediately after Router creation.
     *
     * @param router the root router
     */
    protected void onRootRouterCreate(Router router){}

    /**
     * Called before mounting API subrouter.
     *
     * @param router api subrouter
     */
    protected void beforeAPIRouterMount(Router router){}

    protected void onStart(HttpServer httpServer){}
    protected void onServerDeployFail(Throwable error){}

    protected boolean allowCORSCredentials() { return true; }
    protected Set<HttpMethod> getAllowedMethods() { return  ALLOWED_METHODS; }
    protected Set<String> getAllowedHeaders() { return ALLOWED_HEADERS; }
    protected Set<String> getExposedHeaders() { return EXPOSED_HEADERS; }

    protected Handler<RoutingContext> createRouteLogHandler() {
        return LoggerHandler.create();
    }
    protected Handler<RoutingContext> createRouteFailureHandler() {
        return ErrorHandler.create();
    }

    protected abstract JWTAuth createAuthProvider();
    protected abstract Class<?>[] getResourceClasses();

    private HttpServer createHttpServer() {
        if (appConfig.isProduction()) {
            return vertx.createHttpServer(createSSLHttpOptions());
        }
        return vertx.createHttpServer();
    }

    private HttpServerOptions createSSLHttpOptions () {
        JsonObject ssl = appConfig.getSSLConfig();

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

    private Router buildApiRouter(JWTAuth authProvider) throws RuntimeException {
        Router router = Router.router(vertx);
        beforeAPIRouterMount(router);
        router.route().handler(createRouteLogHandler());
        router.route().failureHandler(createRouteFailureHandler());

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

    private Router buildWebSocketRouter(JWTAuth authProvider) throws RuntimeException {
//        router.route("/*").handler(JWTAuthHandler.create(authProvider));
        SockJSHandler handler = SockJSHandler.create(vertx);
        SockJSBridgeOptions options = new SockJSBridgeOptions();

        List<PermittedOptions> inbound = new ArrayList<>(addInboundSocketRules());
        options.setInboundPermitted(inbound);

        List<PermittedOptions> outbound = new ArrayList<>(addOutboundSocketRules());
        options.setOutboundPermitted(outbound);
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

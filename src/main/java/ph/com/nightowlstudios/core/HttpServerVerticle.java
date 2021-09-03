package ph.com.nightowlstudios.core;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PfxOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ph.com.nightowlstudios.resource.Resource;

import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * @author <a href="mailto:josephharveyangeles@gmail.com">Joseph Harvey Angeles - <i>yev</i></a>
 * @since 4/4/21
 **/
class HttpServerVerticle extends AbstractVerticle {

  private static final Logger log = LoggerFactory.getLogger(HttpServerVerticle.class);

  private final Supplier<Boolean> allowCORSCredentials;
  private final Supplier<Set<HttpMethod>> allowedMethods;
  private final Supplier<Set<String>> allowedHeaders;
  private final Supplier<Set<String>> exposedHeaders;
  private final Supplier<String> apiPrefix;
  private final Supplier<String> webSocketPrefix;
  private final Supplier<String> bannerText;

  private final Consumer<Router> onRouterCreate;
  private final Supplier<Handler<RoutingContext>> createRouteLogHandler;
  private final Supplier<Handler<RoutingContext>> createRouteFailHandler;
  private final Supplier<Class<Resource>[]> getResourceClasses;
  private final Consumer<Router> setupRoutes;

  private final Function<Vertx, Router> createWebSocketRouter;
  private final Consumer<HttpServer> onStart;
  private final Consumer<Throwable> onStartFail;


  HttpServerVerticle(
          Supplier<Boolean> allowCORSCredentials,
          Supplier<Set<HttpMethod>> allowedMethods,
          Supplier<Set<String>> allowedHeaders,
          Supplier<Set<String>> exposedHeaders,
          Supplier<String> apiPrefix,
          Consumer<Router> onRouterCreate,
          Supplier<Handler<RoutingContext>> createRouteLogHandler,
          Supplier<Handler<RoutingContext>> createRouteFailHandler,
          Supplier<Class<Resource>[]> getResourceClasses,
          Consumer<Router> setupRoutes,
          Supplier<String> webSocketRoute,
          Function<Vertx, Router> createWebSocketRouter,
          Supplier<String> bannerText,
          Consumer<HttpServer> onStart,
          Consumer<Throwable> onStartFail
  ) {
    this.allowCORSCredentials = allowCORSCredentials;
    this.allowedMethods = allowedMethods;
    this.allowedHeaders = allowedHeaders;
    this.exposedHeaders = exposedHeaders;
    this.apiPrefix = apiPrefix;
    this.onRouterCreate = onRouterCreate;
    this.createRouteLogHandler = createRouteLogHandler;
    this.createRouteFailHandler = createRouteFailHandler;
    this.getResourceClasses = getResourceClasses;
    this.setupRoutes = setupRoutes;
    this.webSocketPrefix = webSocketRoute;
    this.createWebSocketRouter = createWebSocketRouter;
    this.bannerText = bannerText;
    this.onStart = onStart;
    this.onStartFail = onStartFail;
  }

  private String getAllowedOrigins() {
    return config().getString("allowedOrigins", "*");
  }

  protected int getPort() {
    return config().getInteger("port", 8888);
  }

  protected String appName() {
    return config().getString("name", "edge.api");
  }

  @Override
  public void start(Promise<Void> startPromise) {
    Router rootRouter = Router.router(vertx);
    rootRouter.route().handler(
            CorsHandler
                    .create(getAllowedOrigins())
                    .allowCredentials(allowCORSCredentials.get())
                    .allowedMethods(allowedMethods.get())
                    .allowedHeaders(allowedHeaders.get())
                    .exposedHeaders(exposedHeaders.get())
    );
    rootRouter.route().handler(BodyHandler.create());

    this.onRouterCreate.accept(rootRouter);

    rootRouter.mountSubRouter(apiPrefix.get(), createApiRouter());
    rootRouter.mountSubRouter(webSocketPrefix.get(), createWebSocketRouter.apply(vertx));

    createHttpServer()
            .requestHandler(rootRouter)
            .listen(getPort(), http -> {
              if (http.succeeded()) {
                startPromise.complete();
                log.info(bannerText.get());
                log.info("{} online at PORT: {}", appName(), http.result().actualPort());
                onStart.accept(http.result());
                return;
              }
              log.error("Error running server: " + http.cause());
              startPromise.fail(http.cause());
              onStartFail.accept(http.cause());
            });
  }

  private Router createApiRouter() throws RuntimeException {
    Router router = Router.router(vertx);
    router.route().handler(createRouteLogHandler.get());
    router.route().failureHandler(createRouteFailHandler.get());

    for (Class<Resource> controller : getResourceClasses.get()) {
      try {
        controller.getDeclaredConstructor(Router.class).newInstance(router);
      } catch (Exception e) {
        log.error("Unable to load {}. Error on {}. ", controller.getCanonicalName(), e.getMessage());
        e.printStackTrace();
      }
    }
    setupRoutes.accept(router);
    return router;
  }

  private HttpServer createHttpServer() {
    if (isProduction() && !sslConfig().isEmpty()) {
      return vertx.createHttpServer(createSSLHttpOptions());
    }
    return vertx.createHttpServer();
  }

  private HttpServerOptions createSSLHttpOptions() {
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


}

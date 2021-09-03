package ph.com.nightowlstudios.core;

import io.vertx.core.*;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.ErrorHandler;
import io.vertx.ext.web.handler.LoggerHandler;
import io.vertx.ext.web.handler.sockjs.SockJSBridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ph.com.nightowlstudios.resource.Resource;
import ph.com.nightowlstudios.service.Service;

import java.util.*;

/**
 * @author <a href="mailto:josephharveyangeles@gmail.com">Joseph Harvey Angeles - <i>@yev</i></a>
 * @since 7/14/20
 */
public abstract class ApplicationVerticle extends AbstractVerticle {

  private final HttpServerVerticle serverVerticle;
  private final Map<Class<?>, DeploymentOptions> serviceVerticles;

  public ApplicationVerticle() {
    this.serviceVerticles = new HashMap<>();
    this.serverVerticle = new HttpServerVerticle(
            this::allowCORSCredentials,
            this::allowedMethods,
            this::allowedHeaders,
            this::exposedHeaders,
            this::apiPrefix,
            this::onRouterCreate,
            this::createRouteLogHandler,
            this::createRouteFailureHandler,
            this::getResourceClasses,
            this::setupRoutes,
            this::webSocketRoute,
            this::createWebSocketRouter,
            this::bannerText,
            this::onStart,
            this::onStartFail
    );
  }

  @Override
  public void start(Promise<Void> startPromise) {
    setup();
    CompositeFuture.all(buildDeployList())
      .onSuccess(none -> startPromise.complete())
      .onFailure(failure -> {
        log.error("Unable to deploy Edge Application", failure.getCause());
        startPromise.fail(failure.getMessage());
      });
  }

  /**
   * This is the very first method to be called when launching the main verticle.
   * This is where {@link ph.com.nightowlstudios.service.Service} verticles should be registered using any of the {@link #registerService(Class[])}
   * <p>
   * An Http server verticle is also automatically internally registered to Http requests.
   *
   * @see ph.com.nightowlstudios.service.Service
   */
  public abstract void setup();

  @SuppressWarnings("rawtypes")
  private List<Future> buildDeployList() {
    List<Future> list = new ArrayList<>();
    this.serviceVerticles.forEach((service, options) -> list.add(createLauncher(service, options)));
    list.add(createLauncher(this.serverVerticle));
    return list;
  }

  @SuppressWarnings("unchecked")
  private Future<String> createLauncher(Class<?> service, DeploymentOptions options) {
    // Merge the global `application.yml` config the `DeploymentOptions`
    options.setConfig(Optional
      .ofNullable(options.getConfig())
      .orElse(new JsonObject())
      .mergeIn(config())
    );
    return vertx.deployVerticle((Class<? extends Service>) service, options)
      .onSuccess(none -> log.info("Deployed {} verticle successfully.", service.getName()))
      .onFailure(failure -> log.error("Error deploying {}.", service.getName()));
  }

  private Future<String> createLauncher(Verticle verticle) {
    return vertx.deployVerticle(verticle, new DeploymentOptions().setConfig(config()));
  }

  @SafeVarargs
  protected final <T extends Service> void registerService(Class<T>... services) {
    for (Class<T> service : services) {
      this.registerService(service, new DeploymentOptions());
    }
  }

  protected <T extends Service> void registerService(Class<T> service, DeploymentOptions options) {
    this.serviceVerticles.put(service, options);
    Edge.registerService(service);
  }

  /**
   * Called immediately after Router creation.
   *
   * @param router the root router
   */
  protected void onRouterCreate(Router router) {
  }

  /**
   * Called before mounting API subrouter.
   *
   * @param apiRouter api subrouter
   */
  protected void setupRoutes(Router apiRouter) {
  }

  /**
   * Called when the HttpServer has been started.
   *
   * @param httpServer the started server
   */
  protected void onStart(HttpServer httpServer) {
  }

  /**
   * Called when the HttpServer failed to start
   *
   * @param error cause of the failure
   */
  protected void onStartFail(Throwable error) {
  }

  protected final String appName() {
    return config().getString("name", "edge.api");
  }

  protected final String version() {
    return config().getString("version", "1.0");
  }

  protected boolean allowCORSCredentials() {
    return true;
  }

  protected Set<HttpMethod> allowedMethods() {
    return ALLOWED_METHODS;
  }

  protected Set<String> allowedHeaders() {
    return ALLOWED_HEADERS;
  }

  protected Set<String> exposedHeaders() {
    return EXPOSED_HEADERS;
  }

  protected String apiPrefix() {
    String prefix = config().getString("prefix", "api");
    return String.format("/%s/%s", prefix.trim(), version().trim());
  }

  protected String webSocketRoute() {
    return getWebSocketConfig().getString("route");
  }

  protected JsonObject getWebSocketConfig() {
    JsonObject defaultConfig = new JsonObject()
            .put("route", "/ws")
            .put("inbound", "in*")
            .put("outbound", "out*");
    return config().getJsonObject("ws", defaultConfig);
  }

  protected String bannerText() {
    return BANNER_TXT;
  }

  protected Handler<RoutingContext> createRouteLogHandler() {
    return LoggerHandler.create();
  }

  protected Handler<RoutingContext> createRouteFailureHandler() {
    return ErrorHandler.create(getVertx());
  }

  protected abstract <R extends Resource> Class<R>[] getResourceClasses();

  private Router createWebSocketRouter(Vertx vertx) throws RuntimeException {
    SockJSHandler handler = SockJSHandler.create(vertx);
    SockJSBridgeOptions options = new SockJSBridgeOptions();

    List<PermittedOptions> inbound = new ArrayList<>(addInboundSocketRules());
    options.setInboundPermitteds(inbound);

    List<PermittedOptions> outbound = new ArrayList<>(addOutboundSocketRules());
    options.setOutboundPermitteds(outbound);
    return handler.bridge(options);
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

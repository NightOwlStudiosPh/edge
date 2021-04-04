package ph.com.nightowlstudios.service;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;

import java.util.Optional;

/**
 * @author <a href="mailto:josephharveyangeles@gmail.com">Joseph Harvey Angeles - <i>yev</i></a>
 * @since 4/4/21
 **/
public class ServiceBus<T extends Service> {

  private final Class<T> serviceClass;
  private final Vertx vertx;

  public ServiceBus(Class<T> serviceClass) {
    this(Vertx.currentContext().owner(), serviceClass);
  }

  public ServiceBus(Vertx vertx, Class<T> serviceClass) {
    this.vertx = vertx;
    this.serviceClass = serviceClass;
  }

  @SuppressWarnings("unchecked")
  public <S> Future<Optional<S>> request(String action, Object... payload) {
    DeliveryOptions options = new DeliveryOptions().addHeader("action", action);
    JsonObject body = ServiceUtils.buildRequestPayload(payload);
    System.out.println(body.encodePrettily());
    return this.vertx
      .eventBus()
      .<JsonObject>request(this.serviceClass.getName(), body, options)
      .map(message -> {
        try {
          JsonObject responseBody = message.body();
          return !responseBody.isEmpty()
            ? ServiceUtils.unwrapRequestResponse(responseBody)
            : Optional.empty();
        } catch (Exception e) {
          message.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), e.getMessage());
          return Optional.empty();
        }
      });
  }

}

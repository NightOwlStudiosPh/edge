package ph.com.nightowlstudios.service;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author <a href="mailto:josephharveyangeles@gmail.com">Joseph Harvey Angeles - <i>yev</i></a>
 * @since 4/4/21
 **/
public abstract class Service extends AbstractVerticle {

  protected final Logger log;

  public Service() {
    this.log = LoggerFactory.getLogger(this.getClass());
  }

  @SuppressWarnings("unchecked")
  @Override
  public void start() throws Exception {
    setup(vertx);
    vertx.eventBus()
      .<JsonObject>consumer(this.getClass().getName())
      .handler(message -> {
        String action = message.headers().get("action");
        try {
          JsonObject body = message.body();
          Method method = this.getClass().getMethod(action, ServiceUtils.extractRequestPayloadParameterTypes(body));
          Future<Object> response = (Future<Object>) method.invoke(this, ServiceUtils.extractRequestPayloadParameters(body));
          response.onSuccess(payload -> {
            if (!payload.getClass().getName().equals(Void.class.getName())) {
              message.reply(ServiceUtils.buildReplyPayload(payload));
            }
          }).onFailure(failure -> message.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), failure.getMessage()));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassNotFoundException e) {
          log.error(String.format("Error encountered upon handling of %s action on %s service.", action, this.getClass().getName()), e.getCause());
          message.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), e.getMessage());
        }
      });
    super.start();
  }

  /**
   * Initialize <code>Vert.x</code>-dependent dependencies here. ie: {@link ph.com.nightowlstudios.persistence.PersistenceClient}
   *
   * @param vertx Reference to the <code>Vert.x</code> instance that deployed this verticle.
   */
  protected abstract void setup(Vertx vertx);

}

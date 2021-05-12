package ph.com.nightowlstudios.core;

import io.vertx.core.Vertx;
import ph.com.nightowlstudios.service.Service;
import ph.com.nightowlstudios.service.ServiceBus;

/**
 * @author <a href="mailto:josephharveyangeles@gmail.com">Joseph Harvey Angeles - <i>yev</i></a>
 * @since 4/4/21
 **/
public class Edge {

  private Edge() {
  }

  public static <T extends Service> ServiceBus<T> serviceBus(Class<T> serviceClass) {
    return new ServiceBus<>(serviceClass);
  }

  public static <T extends Service> ServiceBus<T> serviceBus(Vertx vertx, Class<T> serviceClass) {
    return new ServiceBus<>(vertx, serviceClass);
  }
}

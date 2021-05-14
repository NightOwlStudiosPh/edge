package ph.com.nightowlstudios.core;

import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ph.com.nightowlstudios.service.Service;
import ph.com.nightowlstudios.service.ServiceBus;

import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:josephharveyangeles@gmail.com">Joseph Harvey Angeles - <i>yev</i></a>
 * @since 4/4/21
 **/
public class Edge {

  private static final Logger logger = LoggerFactory.getLogger(Edge.class);

  private static final Set<String> registeredServices = new HashSet<>();

  private Edge() {}

  public static <T extends Service> ServiceBus<T> serviceBus(Class<T> serviceClass) {
    if (registeredServices.contains(serviceClass.getName())) {
      return new ServiceBus<>(serviceClass);
    }
    throw runtimeError(serviceClass);
  }

  public static <T extends Service> ServiceBus<T> serviceBus(Vertx vertx, Class<T> serviceClass) {
    if (registeredServices.contains(serviceClass.getName())) {
      return new ServiceBus<>(vertx, serviceClass);
    }
    throw runtimeError(serviceClass);
  }

  static RuntimeException runtimeError(Class<?> serviceClass) {
    logger.error("Unknown {} service. Make sure the service has been registered.", serviceClass.getName());
    return new RuntimeException("Unknown Service");
  }

  static void registerService(Class<?> serviceClass) {
    registeredServices.add(serviceClass.getName());
  }
}

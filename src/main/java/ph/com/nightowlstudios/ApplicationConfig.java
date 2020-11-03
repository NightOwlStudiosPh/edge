package ph.com.nightowlstudios;

import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

/**
 * Application Configuration Object
 *
 * @author <a href="mailto:josephharveyangeles@gmail.com">Joseph Harvey Angeles - <i>@yev</i></a>
 * @since 11/2/20
 */
public interface ApplicationConfig {

    default String getApplicationName() {
        return "NOS-Edge";
    }

    default int getPort() {
        return 8888;
    }

    default String getApiPrefix() {
        return "/api";
    }

    default String getWebSocketPrefix() { return "/eventbus"; }

    default String getBannerText () {
        return ApplicationVerticle.BANNER_TXT;
    }

    default boolean isProduction () {
        Optional<String> env = Optional.ofNullable(System.getenv("ENV"));
        return env.orElse(StringUtils.EMPTY).equalsIgnoreCase("production");
    }

    String getAllowedOrigins();

    JsonObject getSSLConfig();
}

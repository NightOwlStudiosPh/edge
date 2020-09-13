package ph.com.nightowlstudios.resource;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.ext.web.RoutingContext;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author <a href="mailto:josephharveyangeles@gmail.com">Joseph Harvey Angeles - <i>@yev</i></a>
 * @since 7/6/20
 */
public interface Validator<T> {
    Predicate<T> predicate();
    Function<RoutingContext, T> requestMapper();
    HttpResponseStatus responseStatus();
    String errorMessage();
}

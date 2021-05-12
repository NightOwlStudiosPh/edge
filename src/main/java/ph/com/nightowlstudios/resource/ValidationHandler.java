package ph.com.nightowlstudios.resource;

/**
 * @author <a href="mailto:josephharveyangeles@gmail.com">Joseph Harvey Angeles - <i>@yev</i></a>
 * @since 9/12/20
 */

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * @author <a href="mailto:josephharveyangeles@gmail.com">Joseph Harvey Angeles - <i>@yev</i></a>
 * @since 7/6/20
 */
public class ValidationHandler {

  public static Handler<RoutingContext> create(String... requiredParams) {
    return ctx -> {
      JsonObject body = ctx.getBodyAsJson();
      for (String param : requiredParams) {
        if (!body.containsKey(param)) {
          ctx.fail(HttpResponseStatus.BAD_REQUEST.code());
          ctx.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).end("Missing required parameter");
          return;
        }
      }
      ctx.next();
    };
  }

  public static Handler<RoutingContext> create(Validator<JsonObject> validator) {
    return ctx -> {
      JsonObject body = ctx.getBodyAsJson();
      if (!validator.predicate().test(body)) {
        ctx.fail(HttpResponseStatus.PRECONDITION_FAILED.code());
        ctx.response().setStatusCode(HttpResponseStatus.PRECONDITION_FAILED.code()).end(validator.errorMessage());
        return;
      }
      ctx.next();
    };
  }
}


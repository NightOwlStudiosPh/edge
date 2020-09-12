package resource;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:josephharveyangeles@gmail.com">Joseph Harvey Angeles - <i>@{yev}</i></a>
 * @since 7/6/20
 */
public final class Validators {

    private Validators() { }

    public static Handler<RoutingContext> requiredBodyParams(String... params) {
        List<Validator<JsonObject>> validators = Arrays
                .stream(params)
                .map(param -> new ValidatorImpl<>(
                        RoutingContext::getBodyAsJson,
                        body -> body.containsKey(param),
                        HttpResponseStatus.PRECONDITION_REQUIRED))
                .collect(Collectors.toList());
        return ofList(validators);
    }

    public static Handler<RoutingContext> requiredPathParams(String... params) {
        List<Validator<HttpServerRequest>> validators = Arrays
                .stream(params)
                .map(param -> new ValidatorImpl<>(
                        RoutingContext::request,
                        request -> Optional.ofNullable(request.getParam(param)).isPresent(),
                        HttpResponseStatus.PRECONDITION_REQUIRED))
                .collect(Collectors.toList());
        return ofList(validators);
    }

    public static Handler<RoutingContext> stringEqualBodyParams(String paramOne, String paramTwo) {
        Predicate<JsonObject> check = body -> body.getString(paramOne).equals(body.getString(paramTwo));
        return Validators.of(new ValidatorImpl<>(RoutingContext::getBodyAsJson, check, "Passwords do not match."));
    }

    public static <T> Handler<RoutingContext> of(Function<RoutingContext, T> requestMapper, Predicate<T> predicate, String errorMessage) {
        return Validators.of(new ValidatorImpl<>(requestMapper, predicate, errorMessage));
    }

    public static <T> Handler<RoutingContext> of(Validator<T> validator) {
        return ctx -> {
            if (!validator.predicate().test(validator.requestMapper().apply(ctx))) {
                ctx.response().setStatusCode(HttpResponseStatus.PRECONDITION_FAILED.code()).end(validator.errorMessage());
                ctx.fail(HttpResponseStatus.PRECONDITION_FAILED.code());
                return;
            }
            ctx.next();
        };
    }

    public static <T> Handler<RoutingContext> ofList(List<Validator<T>> validators) {
        return ctx -> {
            for (Validator<T> validator :validators) {
                if (!validator.predicate().test(validator.requestMapper().apply(ctx))) {
                    ctx.fail(validator.responseStatus().code());
                    return;
                }
            }
            ctx.next();
        };
    }

    static class ValidatorImpl<T> implements Validator<T> {
        private final Predicate<T> predicate;
        private final Function<RoutingContext,T> requestMapper;
        private final HttpResponseStatus responseStatus;
        private String errorMessage;

        ValidatorImpl(Function<RoutingContext, T> mapper, Predicate<T> predicate, String errorMessage) {
            this(mapper, predicate, HttpResponseStatus.PRECONDITION_FAILED);
            this.errorMessage = String.format("Error %d: %s", this.responseStatus.code(), errorMessage);
        }

        ValidatorImpl(Function<RoutingContext, T> mapper, Predicate<T> predicate, HttpResponseStatus responseStatus) {
            this.requestMapper = mapper;
            this.predicate = predicate;
            this.responseStatus = responseStatus;
            this.errorMessage = responseStatus.reasonPhrase();
        }

        @Override
        public Predicate<T> predicate() {
            return this.predicate;
        }

        @Override
        public Function<RoutingContext, T> requestMapper() {
            return this.requestMapper;
        }

        @Override
        public HttpResponseStatus responseStatus() {
            return this.responseStatus;
        }

        @Override
        public String errorMessage() {
            return this.errorMessage;
        }
    }
}

package ph.com.nightowlstudios.auth;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.KeyStoreOptions;
import io.vertx.ext.auth.authorization.PermissionBasedAuthorization;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.auth.jwt.authorization.JWTAuthorization;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.AuthorizationHandler;
import io.vertx.ext.web.handler.JWTAuthHandler;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:josephharveyangeles@gmail.com">Joseph Harvey Angeles - <i>@yev</i></a>
 * @since 1/13/21
 */
public class BasicAuthentication {

    public static final String CLAIM = "permissions";
    private static BasicAuthentication theInstance = null;

    private final JWTAuth jwtAuth;
    private final JsonObject config;

    private BasicAuthentication() {
        Vertx context = Vertx.currentContext().owner();
        this.config = context.getOrCreateContext().config().getJsonObject("keystore");
        this.jwtAuth = JWTAuth.create(
                context, new JWTAuthOptions()
                    .setKeyStore(new KeyStoreOptions()
                            .setType(this.config.getString("type"))
                            .setPath(this.config.getString("path"))
                            .setPassword(this.config.getString("password"))
                    )
        );
    }

    public static BasicAuthentication getInstance() {
        if (theInstance == null) {
            theInstance = new BasicAuthentication();
        }
        return theInstance;
    }

    public JWTAuth getAuthProvider () {
        return this.jwtAuth;
    }

    public String generateToken(JsonObject claims, UserRole userRole) {
        JWTOptions options = new JWTOptions()
                .setAlgorithm(this.config.getString("algorithm"))
                .setPermissions(Arrays
                    .stream(UserRole.values())
                    .filter(role -> userRole.ordinal() >= role.ordinal())
                    .map(Enum::toString)
                    .collect(Collectors.toList()));
        return theInstance.jwtAuth.generateToken(claims, options);
    }

    public Handler<RoutingContext> createAuthNHandler() {
        return JWTAuthHandler.create(this.jwtAuth);
    }

    public Handler<RoutingContext> createAuthZHandler(UserRole role) {
        return AuthorizationHandler
            .create(PermissionBasedAuthorization.create(role.toString()))
            .addAuthorizationProvider(JWTAuthorization.create(CLAIM));
    }
}

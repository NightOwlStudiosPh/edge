package ph.com.nightowlstudios.resource;

import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.Router;
import ph.com.nightowlstudios.persistence.PersistenceClient;

/**
 * @author <a href="mailto:josephharveyangeles@gmail.com">Joseph Harvey Angeles - <i>@yev</i></a>
 * @since 11/20/20
 */
public abstract class DatabaseResource extends Resource {

    private final PersistenceClient dbClient;

    public DatabaseResource(Router router, JWTAuth authProvider) {
        super(router, authProvider);
        this.dbClient = createDbClient();
    }

    protected PersistenceClient createDbClient() {
        return new PersistenceClient();
    }

    protected PersistenceClient dbClient() { return this.dbClient; }
}

package ph.com.nightowlstudios.resource;

import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.Router;
import ph.com.nightowlstudios.persistence.PersistenceClient;

/**
 * @author <a href="mailto:josephharveyangeles@gmail.com">Joseph Harvey Angeles - <i>@yev</i></a>
 * @since 11/20/20
 */
public abstract class DatabaseResource extends Resource {

    private PersistenceClient dbClient;

    public DatabaseResource(Router router, JWTAuth authProvider) {
        super(router, authProvider);
    }

    @Override
    void init() {
        this.dbClient = this.createDbClient();
    }

    /**
     * Creates default persistence client based from config.
     * Override if using a different client.
     * @return database client
     */
    protected PersistenceClient createDbClient() {
        return new PersistenceClient();
    }

    protected PersistenceClient dbClient() { return this.dbClient; }
}

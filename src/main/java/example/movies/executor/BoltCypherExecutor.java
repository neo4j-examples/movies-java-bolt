package example.movies.executor;

import org.neo4j.driver.*;

import java.util.List;
import java.util.Map;

/**
 * @author Michael Hunger @since 22.10.13
 */
public class BoltCypherExecutor implements CypherExecutor {

    private final org.neo4j.driver.Driver driver;
    private final String database;

    public BoltCypherExecutor(String url) {
        this(url, null, null, null);
    }

    public BoltCypherExecutor(String url, String username, String password, String database) {
        boolean hasPassword = password != null && !password.isEmpty();
        AuthToken token = hasPassword ? AuthTokens.basic(username, password) : AuthTokens.none();
        driver = GraphDatabase.driver(url, token);
        this.database = database;
    }

    @Override
    public List<Map<String, Object>> query(String query, Map<String, Object> params) {
        try (Session session = getSession()) {
            return session.readTransaction(
               tx -> tx.run(query, params).list( r -> r.asMap(BoltCypherExecutor::convert))
            );
        }
    }

    private Session getSession() {
        if (database == null || database.isBlank()) return driver.session();
        return driver.session(SessionConfig.forDatabase(database));
    }

    static Object convert(Value value) {
        switch (value.type().name()) {
            case "PATH":
                return value.asList(BoltCypherExecutor::convert);
            case "NODE":
            case "RELATIONSHIP":
                return value.asMap();
        }
        return value.asObject();
    }

}

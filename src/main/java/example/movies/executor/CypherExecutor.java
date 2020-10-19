package example.movies.executor;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.Value;

import java.util.List;
import java.util.Map;

/**
 * @author Michael Hunger @since 22.10.13
 */
public class CypherExecutor {

    private final Driver driver;

    private final String database;

    public CypherExecutor(String url, String username, String password, String database) {
        this.driver = GraphDatabase.driver(
                url,
                AuthTokens.basic(username, password));
        this.database = database;
    }

    public List<Map<String, Object>> query(String query, Map<String, Object> params) {
        try (Session session = getSession()) {
            return session.readTransaction(
               tx -> tx.run(query, params).list( r -> r.asMap(CypherExecutor::convert))
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
                return value.asList(CypherExecutor::convert);
            case "NODE":
            case "RELATIONSHIP":
                return value.asMap();
        }
        return value.asObject();
    }

}

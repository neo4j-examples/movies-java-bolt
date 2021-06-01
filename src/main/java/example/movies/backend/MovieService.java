package example.movies.backend;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.Value;
import org.neo4j.driver.summary.ResultSummary;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author mh
 * @since 30.05.12
 */
public class MovieService {

    private final Driver driver;

    private final String database;

    public MovieService(Driver driver, String database) {
        this.driver = driver;
        this.database = database;
    }

    public Map<String, Object> findMovie(String title) {
        if (title == null) return Map.of();
        var result = query(
                "MATCH (movie:Movie {title:$title})" +
                        " OPTIONAL MATCH (movie)<-[r]-(person:Person)\n" +
                        " RETURN movie.title as title, collect({name:person.name, job:head(split(toLower(type(r)),'_')), role:r.roles}) as cast LIMIT 1",
                Map.of("title", title));
        return result.isEmpty() ? Map.of() : result.get(0);
    }

    public Integer voteInMovie(String title) {
        if (title == null) return 0;
        try (Session session = getSession()) {
            return session.writeTransaction( tx -> {
                Result result = tx.run(
                        "MATCH (m:Movie {title: $title}) " +
                        "WITH m, (CASE WHEN exists(m.votes) THEN m.votes ELSE 0 END) AS currentVotes " +
                        "SET m.votes = currentVotes + 1;", Map.of("title", title) );
                ResultSummary summary = result.consume();
                return summary.counters().propertiesSet();
            } );
        }
    }

    public Iterable<Map<String, Object>> search(String query) {
        if (query == null || query.trim().isEmpty()) return Collections.emptyList();
        return query(
                "MATCH (movie:Movie)\n" +
                        " WHERE toLower(movie.title) CONTAINS $part\n" +
                        " RETURN movie",
                Map.of("part", query.toLowerCase()));
    }

    public Map<String, Object> graph(int limit) {
        var result = query(
                "MATCH (m:Movie)<-[:ACTED_IN]-(a:Person) " +
                        " RETURN m.title as movie, collect(a.name) as cast " +
                        " LIMIT $limit", Map.of("limit", limit));

        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> rels = new ArrayList<>();
        int i = 0;
        for (var row : result) {
            nodes.add(Map.of("title", row.get("movie"), "label", "movie"));
            int target = i;
            i++;
            for (Object name : (Collection<?>) row.get("cast")) {
                Map<String, Object> actor = Map.of("title", name, "label", "actor");
                int source = nodes.indexOf(actor);
                if (source == -1) {
                    nodes.add(actor);
                    source = i++;
                }
                rels.add(Map.of("source", source, "target", target));
            }
        }
        return Map.of("nodes", nodes, "links", rels);
    }

    private List<Map<String, Object>> query(String query, Map<String, Object> params) {
        try (Session session = getSession()) {
            return session.readTransaction(
                    tx -> tx.run(query, params).list(r -> r.asMap(MovieService::convert))
            );
        }
    }

    private Session getSession() {
        if (database == null || database.isBlank()) return driver.session();
        return driver.session(SessionConfig.forDatabase(database));
    }

    private static Object convert(Value value) {
        switch (value.type().name()) {
            case "PATH":
                return value.asList(MovieService::convert);
            case "NODE":
            case "RELATIONSHIP":
                return value.asMap();
        }
        return value.asObject();
    }
}

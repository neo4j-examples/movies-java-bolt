package example.movies.backend;

import example.movies.executor.CypherExecutor;

import java.util.*;

/**
 * @author mh
 * @since 30.05.12
 */
public class MovieService {

    private final CypherExecutor cypher;

    public MovieService(String url, String username, String password, String database) {
        cypher = createCypherExecutor(url, username, password, database);
    }

    private CypherExecutor createCypherExecutor(String url, String username, String password, String database) {
        return new CypherExecutor(url, username, password, database);
    }

    public Map<String, Object> findMovie(String title) {
        if (title == null) return Map.of();
        var result = cypher.query(
                "MATCH (movie:Movie {title:$title})" +
                        " OPTIONAL MATCH (movie)<-[r]-(person:Person)\n" +
                        " RETURN movie.title as title, collect({name:person.name, job:head(split(toLower(type(r)),'_')), role:r.roles}) as cast LIMIT 1",
                Map.of("title", title));
        return result.isEmpty() ? Map.of() : result.get(0);
    }

    public Iterable<Map<String, Object>> search(String query) {
        if (query == null || query.trim().isEmpty()) return Collections.emptyList();
        return cypher.query(
                "MATCH (movie:Movie)\n" +
                        " WHERE toLower(movie.title) CONTAINS $part\n" +
                        " RETURN movie",
                Map.of("part", query.toLowerCase()));
    }

    public Map<String, Object> graph(int limit) {
        var result = cypher.query(
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
}

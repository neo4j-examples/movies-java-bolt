package example.movies.backend;

import example.movies.executor.CypherExecutor;
import example.movies.executor.BoltCypherExecutor;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * @author mh
 * @since 30.05.12
 */
public class MovieService {

    private final CypherExecutor cypher;

    public MovieService(String uri) {
        cypher = createCypherExecutor(uri);
    }

    private CypherExecutor createCypherExecutor(String uri) {
        try {
            URL url = new URL(uri.replaceAll("(bolt|neo4j(\\+s|\\+scc)?)", "http"));
            String auth = url.getUserInfo();
            String database = url.getPath();
            if (database != null && !database.isBlank()) database = database.substring(1);
            if (auth != null) {
                String[] parts = auth.split(":");
                String boltUrl = uri.replace(auth + "@", "").replace("/" + database, "");
                return new BoltCypherExecutor(boltUrl,parts[0],parts[1],database);
            }
            return new BoltCypherExecutor(uri);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid Neo4j-ServerURL " + uri);
        }
    }

    public Map findMovie(String title) {
        if (title==null) return Map.of();
        var result = cypher.query(
                "MATCH (movie:Movie {title:$title})" +
                " OPTIONAL MATCH (movie)<-[r]-(person:Person)\n" +
                " RETURN movie.title as title, collect({name:person.name, job:head(split(toLower(type(r)),'_')), role:r.roles}) as cast LIMIT 1",
                Map.of("title", title));
        return result.isEmpty() ? Map.of() : result.get(0);
    }

    @SuppressWarnings("unchecked")
    public Iterable<Map<String,Object>> search(String query) {
        if (query==null || query.trim().isEmpty()) return Collections.emptyList();
        return cypher.query(
                "MATCH (movie:Movie)\n" +
                " WHERE toLower(movie.title) CONTAINS $part\n" +
                " RETURN movie",
                Map.of("part", query.toLowerCase()));
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> graph(int limit) {
        var result = cypher.query(
                "MATCH (m:Movie)<-[:ACTED_IN]-(a:Person) " +
                " RETURN m.title as movie, collect(a.name) as cast " +
                " LIMIT $limit", Map.of("limit",limit));
        List nodes = new ArrayList();
        List rels= new ArrayList();
        int i=0;
        for (var row : result) {
            nodes.add(Map.of("title",row.get("movie"),"label","movie"));
            int target=i;
            i++;
            for (Object name : (Collection) row.get("cast")) {
                Map<String, Object> actor = Map.of("title", name,"label","actor");
                int source = nodes.indexOf(actor);
                if (source == -1) {
                    nodes.add(actor);
                    source = i++;
                }
                rels.add(Map.of("source",source,"target",target));
            }
        }
        return Map.of("nodes", nodes, "links", rels);
    }
}

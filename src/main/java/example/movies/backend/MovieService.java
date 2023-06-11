package example.movies.backend;

import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.neo4j.driver.Driver;
import org.neo4j.driver.QueryConfig;
import org.neo4j.driver.Value;

/**
 * @author mh
 * @since 30.05.12
 */
public class MovieService {

    private final Driver driver;

    private final QueryConfig queryConfig;

    public MovieService(Driver driver, String database) {
        this.driver = driver;
        var builder = QueryConfig.builder();
        if (database != null && !database.isBlank()) {
            builder.withDatabase(database);
        }
        this.queryConfig = builder.build();
    }

    public Map<String, Object> findMovie(String title) {
        if (title == null) return Map.of();
        var result = query(
                """
                        MATCH (movie:Movie {title:$title})
                        OPTIONAL MATCH (movie)<-[r]-(person:Person)
                        RETURN movie.title as title, collect({name:person.name, job:head(split(toLower(type(r)),'_')), role:r.roles}) as cast LIMIT 1""",
                Map.of("title", title));
        return result.isEmpty() ? Map.of() : result.get(0);
    }

    public Integer voteInMovie(String title) {
        if (title == null) return 0;
        var result = driver.executableQuery(
                        """
                          MATCH (m:Movie {title: $title})
                          SET m.votes = COALESCE(m.votes, 0) + 1;""")
                .withParameters(Map.of("title", title))
                .withConfig(queryConfig)
                .execute();
        return result.summary().counters().propertiesSet();
    }

    public Iterable<Map<String, Object>> search(String query) {
        if (query == null || query.trim().isEmpty()) return Collections.emptyList();
        return query(
                """
                        MATCH (movie:Movie)
                        WHERE toLower(movie.title) CONTAINS toLower($part)
                        RETURN movie""",
                Map.of("part", query));
    }

    public Map<String, Object> graph(int limit) {
        var result = query(
                """
                        MATCH (m:Movie)<-[:ACTED_IN]-(a:Person)
                        RETURN m.title as movie, collect(a.name) as cast
                        LIMIT $limit""",
                Map.of("limit", limit));

        var nodes = new ArrayList<Map<String, Object>>();
        var rels = new ArrayList<Map<String, Object>>();
        var i = 0;
        for (var row : result) {
            nodes.add(Map.of("title", row.get("movie"), "label", "movie"));
            var target = i;
            i++;
            for (Object name : (Collection<?>) row.get("cast")) {
                var actor = Map.of("title", name, "label", "actor");
                var source = nodes.indexOf(actor);
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
        return driver.executableQuery(query)
                .withParameters(params)
                .withConfig(queryConfig)
                .execute(mapping(r -> r.asMap(MovieService::convert), toList()));
    }

    private static Object convert(Value value) {
        return switch (value.type().name()) {
            case "PATH" -> value.asList(MovieService::convert);
            case "NODE", "RELATIONSHIP" -> value.asMap();
            default -> value.asObject();
        };
    }
}

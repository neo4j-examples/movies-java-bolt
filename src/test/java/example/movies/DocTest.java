package example.movies;

import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;

import static java.util.Collections.singletonMap;

/**
 * @author mh
 * @since 14.04.16
 */
public class DocTest {
    public static void main(String[] args) {
        Driver driver = GraphDatabase.driver("bolt://localhost");
        String query = "MATCH (:Movie {title:{title}})<-[:ACTED_IN]-(a:Person) RETURN a.name as actor";

        try (Session session = driver.session()) {

            StatementResult result = session.run(query, singletonMap("title", "The Matrix"));
            while (result.hasNext()) {
                System.out.println(result.next().get("actor"));
            }
        }
    }
}

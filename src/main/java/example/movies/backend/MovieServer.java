package example.movies.backend;

import example.movies.Environment;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.GraphDatabase;
import spark.Spark;

import static spark.Spark.staticFileLocation;

/**
 * @author Michael Hunger @since 22.10.13
 */
public class MovieServer {

    public static void main(String[] args) {
        Spark.port(Environment.getPort());
        staticFileLocation("/public");
        var driver = GraphDatabase.driver(
                Environment.getNeo4jUrl(),
                AuthTokens.basic(Environment.getNeo4jUsername(), Environment.getNeo4jPassword())
        );
        var service = new MovieService(driver, Environment.getNeo4jDatabase());
        new MovieRoutes(service).init();

        Runtime.getRuntime().addShutdownHook(new Thread(driver::close));
    }
}

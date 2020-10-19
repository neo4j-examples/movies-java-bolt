package example.movies.backend;

import example.movies.Environment;
import spark.Spark;

import static spark.Spark.externalStaticFileLocation;

/**
 * @author Michael Hunger @since 22.10.13
 */
public class MovieServer {

    public static void main(String[] args) {
        Spark.port(Environment.getPort());
        externalStaticFileLocation("src/main/webapp");
        MovieService service = new MovieService(
                Environment.getNeo4jUrl(),
                Environment.getNeo4jUsername(),
                Environment.getNeo4jPassword(),
                Environment.getNeo4jDatabase()
        );
        new MovieRoutes(service).init();
    }
}

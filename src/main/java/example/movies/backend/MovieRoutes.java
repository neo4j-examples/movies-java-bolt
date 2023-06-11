package example.movies.backend;

import static spark.Spark.get;
import static spark.Spark.post;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import spark.servlet.SparkApplication;

public class MovieRoutes implements SparkApplication {

    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    private final MovieService service;

    public MovieRoutes(MovieService service) {
        this.service = service;
    }

    public void init() {
        get(
                "/movie/:title",
                (req, res) ->
                        gson.toJson(service.findMovie(URLDecoder.decode(req.params("title"), StandardCharsets.UTF_8))));
        post("/movie/vote/:title", (req, res) -> {
            Integer updates = service.voteInMovie(URLDecoder.decode(req.params("title"), StandardCharsets.UTF_8));
            return gson.toJson(Map.of("updated", updates));
        });
        get("/search", (req, res) -> gson.toJson(service.search(req.queryParams("q"))));
        get("/graph", (req, res) -> {
            int limit = req.queryParams("limit") != null ? Integer.parseInt(req.queryParams("limit")) : 100;
            return gson.toJson(service.graph(limit));
        });
    }
}

package example.movies.backend;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.servlet.SparkApplication;

import java.net.URLDecoder;

import static spark.Spark.get;

public class MovieRoutes implements SparkApplication {

    private Gson gson = new GsonBuilder().disableHtmlEscaping().create();
    private MovieService service;

    public MovieRoutes(MovieService service) {
        this.service = service;
    }

    public void init() {
        get("/movie/:title", (req, res) -> gson.toJson(service.findMovie(URLDecoder.decode(req.params("title")))));
        get("/search", (req, res) -> gson.toJson(service.search(req.queryParams("q"))));
        get("/graph", (req, res) -> {
            int limit = req.queryParams("limit") != null ? Integer.valueOf(req.queryParams("limit")) : 100;
            return gson.toJson(service.graph(limit));
        });
    }
}

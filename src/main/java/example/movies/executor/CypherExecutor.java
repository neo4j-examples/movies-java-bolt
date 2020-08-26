package example.movies.executor;

import java.util.List;
import java.util.Map;

/**
 * @author Michael Hunger @since 22.10.13
 */
public interface CypherExecutor {
    List<Map<String, Object>> query(String statement, Map<String,Object> params);
}

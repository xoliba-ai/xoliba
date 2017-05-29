/*
 * This Java source file was generated by the Gradle 'init' task.
 */
import static spark.Spark.*;
import spark.Spark;
import spark.utils.IOUtils;
import spark.Request;
import java.util.Map;
import java.util.HashMap;


public class App {
	
    public static void main(String[] args) {
        if (System.getenv("PORT") != null) {
            port(Integer.valueOf(System.getenv("PORT")));
        }
		
		staticFiles.location("/public");

		webSocket("/ai", AiWebSocket.class);
		
        get("/", (req, res) -> {
            res.redirect("http://xoliba.herokuapp.com");
            return null;
        });
    }
}

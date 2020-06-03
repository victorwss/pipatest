package ninja.javahacker.temp.pipatest;

import com.fasterxml.jackson.databind.DeserializationFeature;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.plugin.json.JavalinJackson;
import io.javalin.plugin.json.JavalinJson;
import io.javalin.plugin.openapi.OpenApiOptions;
import io.javalin.plugin.openapi.OpenApiPlugin;
import io.javalin.plugin.openapi.annotations.HttpMethod;
import io.javalin.plugin.openapi.annotations.OpenApi;
import io.javalin.plugin.openapi.annotations.OpenApiContent;
import io.javalin.plugin.openapi.annotations.OpenApiParam;
import io.javalin.plugin.openapi.annotations.OpenApiRequestBody;
import io.javalin.plugin.openapi.annotations.OpenApiResponse;
import io.javalin.plugin.openapi.ui.SwaggerOptions;
import io.swagger.v3.oas.models.info.Info;
import ninja.javahacker.temp.pipatest.data.HighscoresTableData;
import ninja.javahacker.temp.pipatest.data.PositionedUserData;
import ninja.javahacker.temp.pipatest.data.UserData;

/**
 * This class is the controller responsible for receiving the HTTP requests for the HTTP-based game highscores table.
 * @author Victor Williams Stafusa da Silva
 */
@SuppressFBWarnings("IMC_IMMATURE_CLASS_NO_TOSTRING")
public class GameServer {

    /**
     * The highscore table.
     */
    @NonNull
    private final HighscoresTable table;

    /**
     * Object responsible for actually serving the HTTP requests.
     */
    @NonNull
    private final Javalin server;

    /**
     * HTTP port tht this request is listening.
     */
    private final int port;

    /**
     * Starts the game server.
     * @param port The HTTP port which should be used to run the server.
     */
    public GameServer(int port) {
        this.port = port;

        // Configure Javalin's Jackson instance to make it very strict in rejecting bad JSONs.
        JavalinJackson
                .getObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, true)
                .configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, true)
                .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true)
                .configure(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES, true)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
                .configure(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY, true)
                .findAndRegisterModules();

        // Intantiate the highscores table.
        this.table = HighscoresTable.getSynchronizedImplementation();

        // Instantiates the server with the configured routes.
        this.server = Javalin
                .create(cfg -> {
                    cfg.registerPlugin(getOpenApiPlugin());
                    cfg.defaultContentType = "application/json";
                    cfg.showJavalinBanner = false;
                })
                .post("/score", this::addScore)
                .get("/score/:userId/position", this::findUser)
                .get("/highscorelist", this::getHighScores)
                .start(port);
    }

    /**
     * Stops the server.
     */
    public void stop() {
        server.stop();
    }

    /**
     * Handle the POST "/score" route.
     * @param ctx The Javalin's context.
     */
    @OpenApi(
            summary = "Post a user's score points.",
            operationId = "addScore",
            description = "This method can be called several times per user and not return anything. "
                    + "The points should be added to the user’s current score (score = current score + new points).",
            path = "/score",
            method = HttpMethod.POST,
            requestBody = @OpenApiRequestBody(content = @OpenApiContent(from = UserData.class)),
            responses = {
                @OpenApiResponse(status = "200"),
                @OpenApiResponse(status = "422")
            }
    )
    private void addScore(@NonNull Context ctx) {
        FunctionUtils.tryRun(() -> JavalinJson.fromJson(ctx.body(), UserData.class), table::addScore, e -> ctx.status(422));
    }

    /**
     * Handle the GET "/score/:user-id/position" route.
     * @param ctx The Javalin's context.
     */
    @OpenApi(
            summary = "Get the current position of a user.",
            operationId = "findUser",
            description = "Retrieves the current position of a specific user, considering the score for all users. "
                    + "If a user hasn't submitted a score, the response must be empty.",
            path = "/score/:userId/position",
            method = HttpMethod.GET,
            pathParams = @OpenApiParam(name = "userId", type = long.class, description = "User's id."),
            responses = {
                @OpenApiResponse(status = "200", content = @OpenApiContent(from = PositionedUserData.class)),
                @OpenApiResponse(status = "404")
            }
    )
    private void findUser(@NonNull Context ctx) {
        FunctionUtils.ifPresentOrElse(
                FunctionUtils.parseOptionalLong(ctx.pathParam("userId")),
                userId -> FunctionUtils.ifPresentOrElse(table.findUser(userId), f -> ctx.json(f), () -> ctx.result("")),
                () -> ctx.status(404)
        );
    }

    /**
     * Handle the GET "/highscorelist" route.
     * @param ctx The Javalin's context.
     */
    @OpenApi(
            summary = "Get a high score list.",
            operationId = "getHighScores",
            description = "Retrieves the high scores list, in order, limited to the 20000 higher scores. "
                    + "A request for a high score list without any scores submitted shall be an empty list.",
            path = "/highscorelist",
            method = HttpMethod.GET,
            responses = @OpenApiResponse(status = "200", content = @OpenApiContent(from = HighscoresTableData.class))
    )
    private void getHighScores(@NonNull Context ctx) {
        ctx.json(table.getHighScores(20000));
    }

    /**
     * Register the swagger's plugin into Javalin.
     * @return Javalin's open API plugin.
     */
    private OpenApiPlugin getOpenApiPlugin() {
        Info info = new Info()
                .version("1.0")
                .title("Highscores game server")
                .description("Highscores game server.");
        OpenApiOptions options = new OpenApiOptions(info)
                .activateAnnotationScanningFor("ninja.javahacker.temp.pipatest")
                .path("/swagger-docs")
                .swagger(new SwaggerOptions("/swagger-ui").title("Highscores game server"));
        return new OpenApiPlugin(options);
    }

    /**
     * Gives the URL where the swagger's documents are published.
     * @return The URL where the swagger's documents are published.
     */
    public String getSwaggerUrl() {
        return "http://localhost:" + port + "/swagger-ui";
    }
}

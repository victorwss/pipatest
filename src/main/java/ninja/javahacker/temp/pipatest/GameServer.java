package ninja.javahacker.temp.pipatest;

import com.fasterxml.jackson.databind.DeserializationFeature;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.javalin.Javalin;
import io.javalin.plugin.json.JavalinJackson;
import io.javalin.plugin.json.JavalinJson;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import ninja.javahacker.temp.pipatest.data.UserData;

/**
 * This class is the controller responsible for receiving the HTTP requests for the HTTP-based game highscores table.
 * @author Victor Williams Stafusa da Silva
 */
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
     * Starts the game server.
     * @param port The HTTP port which should be used to run the server.
     */
    public GameServer(int port) {
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
        this.table = new HighscoresTable();

        // Instantiates the server with the configured routes.
        this.server = Javalin
                .create(cfg -> {
                    cfg.showJavalinBanner = false;
                })
                .post("/score",
                        ctx -> tryRun(
                                () -> JavalinJson.fromJson(ctx.body(), UserData.class),
                                table::addScore,
                                e -> ctx.status(422)
                        )
                )
                .get("/score/:user-id/position",
                        ctx -> ifPresentOrElse(parseOptionalLong(ctx.pathParam("user-id")),
                                userId -> ifPresentOrElse(table.findUser(userId), f -> ctx.json(f), () -> ctx.result("")),
                                () -> ctx.status(404)
                        )
                )
                .get("/highscorelist", ctx -> ctx.json(table.getHighScores(20000)))
                .start(port);
    }

    /**
     * Stops the server.
     */
    public void stop() {
        server.stop();
    }

    /**
     * Parses a {@code long} using the {@link Long#parseLong(String)} method, but returns an {@link Optional} containing the parsed value.
     * If the parse fails, an empty {@link Optional} is returned.
     * @param toParse The value to be parsed as a long.
     * @return An {@link Optional} containing the parsed value or an empty {@link Optional} if the parse fails.
     */
    @NonNull
    private static Optional<Long> parseOptionalLong(@NonNull String toParse) {
        try {
            return Optional.of(Long.parseLong(toParse));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     * If a value is present, performs the given action with the value, otherwise performs the given empty-based action.
     * <p>Java 9 features the {@code Optional.ifPresentOrElse(Consumer<? super X>, Runnable)} method, but since we are working with
     * Java 8, this method provides the functionality of that method.</p>
     * @param <X> The type of the {@link Optional}.
     * @param what The {@link Optional}
     * @param receiver The action to be performed, if a value is present.
     * @param onElse The empty-based action to be performed, if no value is present.
     * @throws NullPointerException If the value itself is {@code null} or if a value is present and the given action is {@code null},
     *     or no value is present and the given empty-based action is {@code null}.
     */
    private static <X> void ifPresentOrElse(@NonNull Optional<X> what, @NonNull Consumer<? super X> receiver, @NonNull Runnable onElse) {
        if (what.isPresent()) {
            receiver.accept(what.get());
        } else {
            onElse.run();
        }
    }

    /**
     * Equivalent of {@link Supplier} where the functional method might also throw any exception in the functional.
     * @param <T> The type of results supplied by this supplier.
     */
    @FunctionalInterface
    private static interface ThrowingSupplier<T> {

        /**
         * Gets a result.
         * @return A result.
         * @throws Throwable If it was not possible to get the result.
         */
        public T get() throws Throwable;
    }

    /**
     * Tries to obtain a value from the given {@link ThrowingSupplier}, and executes one action if it succeeds or the other action if it
     * does not.
     * @param <T> The type of object obtained from the {@code func} {@link ThrowingSupplier}.
     * @param func The supplier that will give out some object.
     * @param ifOk The action to be performed with the value obtained from the {@code func} {@link ThrowingSupplier}.
     * @param cons The action to be performed with the exception obtained from the failure of the execution of the {@code func}
     *     {@link ThrowingSupplier}.
     */
    private static <T> void tryRun(
            @NonNull ThrowingSupplier<T> func,
            @NonNull Consumer<? super T> ifOk,
            @NonNull Consumer<? super Throwable> cons)
    {
        T out;
        try {
            out = func.get();
        } catch (Throwable e) {
            cons.accept(e);
            return;
        }
        ifOk.accept(out);
    }
}

package ninja.javahacker.temp.pipatest.data;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.javalin.plugin.json.JavalinJson;
import java.util.List;
import java.util.Objects;
import net.jcip.annotations.Immutable;

/**
 * Represents the full highscores data given as an output in the JSON format for the {@code GET /highscorelist} HTTP route.
 * @author Victor Williams Stafusa da Silva
 */
@Immutable
public final class HighscoresTableData {

    /**
     * The highscore table.
     */
    @NonNull
    private final List<PositionedUserData> highscores;

    /**
     * Creates an instance from the users' data highscores. It is expected that instances of this class would be serialized as
     * JSON by Jackson.
     * @param highscores The users' highscores.
     * @throws IllegalArgumentException If the parameter is {@code null}.
     */
    public HighscoresTableData(@NonNull List<PositionedUserData> highscores) {
        if (highscores == null) throw new IllegalArgumentException();
        this.highscores = highscores;
    }

    /**
     * Gets the highscore table.
     * @return The highscore table.
     */
    @NonNull
    public List<PositionedUserData> getHighscores() {
        return highscores;
    }

    /**
     * Gives a hash code for this object.
     * @return This object's hash code.
     */
    @Override
    public int hashCode() {
        return Objects.hash(highscores);
    }

    /**
     * Determines if this object is equal to the given object.
     * @param other Another object to be compared as being equal to this one.
     * @return {@code true} if this object is equal to the other, {@code false} otherwise.
     */
    @Override
    public boolean equals(@Nullable Object other) {
        if (!(other instanceof HighscoresTableData)) return false;
        HighscoresTableData that = (HighscoresTableData) other;
        return Objects.equals(this.highscores, that.highscores);
    }

    /**
     * Gives a representation of this object as a JSON string.
     * @return A representation of this object as a JSON string.
     */
    @NonNull
    @Override
    public String toString() {
        return JavalinJson.toJson(this);
    }
}

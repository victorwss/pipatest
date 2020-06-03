package ninja.javahacker.temp.pipatest.data;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.javalin.plugin.json.JavalinJson;
import java.util.Objects;

/**
 * Represents a user data given as an output in the JSON format for the {@code GET /score<user-id>/position} and the
 * {@code GET /highscorelist} HTTP routes.
 * @author Victor Williams Stafusa da Silva
 */
public final class PositionedUserData {

    /**
     * The id of the user.
     */
    private final long userId;

    /**
     * The points earned by the user.
     */
    private final long points;

    /**
     * The position of the user in the highscore. Tied users have the same position.
     */
    private final int position;

    /**
     * Creates an instance from its values. It is expected that instances of this class would be serialized as JSON by Jackson.
     * @param userId The id of the user.
     * @param points The points earned by the user.
     * @param position The position of the user in the highscore. Tied users have the same position.
     * @throws IllegalArgumentException If any parameter have a negative value or if the {@code position} is zero.
     */
    public PositionedUserData(long userId, long points, int position) {
        if (userId < 0 || points < 0 || position <= 0) throw new IllegalArgumentException();
        this.userId = userId;
        this.points = points;
        this.position = position;
    }

    /**
     * Gets the id of the user.
     * @return The id of the user.
     */
    public long getUserId() {
        return userId;
    }

    /**
     * Gets the points earned by the user.
     * @return The points earned by the user.
     */
    public long getPoints() {
        return points;
    }

    /**
     * Gets the position of the user in the highscore. Tied users have the same position.
     * @return The position of the user in the highscore. Tied users have the same position.
     */
    public int getPosition() {
        return position;
    }

    /**
     * Gives a hash code for this object.
     * @return This object's hash code.
     */
    @Override
    public int hashCode() {
        return Objects.hash(position, points, userId);
    }

    /**
     * Determines if this object is equal to the given object.
     * @param other Another object to be compared as being equal to this one.
     * @return {@code true} if this object is equal to the other, {@code false} otherwise.
     */
    @Override
    public boolean equals(@Nullable Object other) {
        if (!(other instanceof PositionedUserData)) return false;
        PositionedUserData that = (PositionedUserData) other;
        return this.position == that.position && this.points == that.points && this.userId == that.userId;
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

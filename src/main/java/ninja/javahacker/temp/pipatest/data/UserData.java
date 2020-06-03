package ninja.javahacker.temp.pipatest.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.javalin.plugin.json.JavalinJson;
import java.util.Objects;
import net.jcip.annotations.Immutable;

/**
 * Represents a user data accepted as an input in the JSON format for the {@code POST /score} HTTP route.
 * @author Victor Williams Stafusa da Silva
 */
@Immutable
public final class UserData {

    /**
     * The id of the user.
     */
    private final long userId;

    /**
     * The points earned by the user.
     */
    private final long points;

    /**
     * Creates an instance from its values. It is expected that this constructor would be invoked reflectively by Jackson as part
     * of JSON deserialization.
     * @param userId The id of the user.
     * @param points The points earned by the user.
     * @throws IllegalArgumentException If either parameter have a negative value.
     */
    @JsonCreator
    public UserData(long userId, long points) {
        if (userId < 0 || points < 0) throw new IllegalArgumentException();
        this.userId = userId;
        this.points = points;
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
     * Gives a hash code for this object.
     * @return This object's hash code.
     */
    @Override
    public int hashCode() {
        return Objects.hash(points, userId);
    }

    /**
     * Determines if this object is equal to the given object.
     * @param other Another object to be compared as being equal to this one.
     * @return {@code true} if this object is equal to the other, {@code false} otherwise.
     */
    @Override
    public boolean equals(@Nullable Object other) {
        if (!(other instanceof UserData)) return false;
        UserData that = (UserData) other;
        return this.points == that.points && this.userId == that.userId;
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

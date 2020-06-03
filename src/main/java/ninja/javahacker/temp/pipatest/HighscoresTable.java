package ninja.javahacker.temp.pipatest;

import edu.umd.cs.findbugs.annotations.CheckReturnValue;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import ninja.javahacker.temp.pipatest.data.HighscoresTableData;
import ninja.javahacker.temp.pipatest.data.PositionedUserData;
import ninja.javahacker.temp.pipatest.data.UserData;

/**
 * Represents the living data of the highscore table.
 * @author Victor Williams Stafusa da Silva
 */
public class HighscoresTable {


    /**
     * Stores the application state.
     */
    @NonNull
    private final AtomicReference<ApplicationState> state;

    /**
     * Creates a new initially empty {@code HighscoreTable}.
     */
    public HighscoresTable() {
        this.state = new AtomicReference<>(new ApplicationState());
    }

    /**
     * Adds the given user points to the application state.
     * @param data The user data containing the user id and the quantity of points that s/he scored.
     */
    public void addScore(@NonNull UserData data) {
        state.updateAndGet(s -> s.addScore(data));
    }

    /**
     * Find the score and the position of a user given by his/her id.
     * @param userId The id of the user we want to find out the score and the position.
     * @return An {@link Optional} containing the user data or an empty one if the user had never been seen before.
     */
    @NonNull
    @CheckReturnValue
    public Optional<PositionedUserData> findUser(long userId) {
        return state.get().findUser(userId);
    }

    /**
     * Creates an object containing a list of the topmost users and their scores and positions. Tied users are shown with the same
     * position ordered by their user id. The user with the most points is presented in position one.
     * @param maxUsers The maximum number of users that we want to get listed.
     * @return An object containing all the users and their scores and positions.
     */
    @NonNull
    @CheckReturnValue
    public HighscoresTableData getHighScores(int maxUsers) {
        return state.get().getHighScores(maxUsers);
    }
}

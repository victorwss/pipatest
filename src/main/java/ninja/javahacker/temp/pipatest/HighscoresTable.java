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
public interface HighscoresTable {

    /**
     * Adds the given user points to the application state.
     * @param data The user data containing the user id and the quantity of points that s/he scored.
     * @throws IllegalArgumentException If the {@code data} is {@code null}.
     */
    public void addScore(@NonNull UserData data);

    /**
     * Find the score and the position of a user given by his/her id.
     * @param userId The id of the user we want to find out the score and the position.
     * @return An {@link Optional} containing the user data or an empty one if the user had never been seen before.
     */
    @NonNull
    @CheckReturnValue
    public Optional<PositionedUserData> findUser(long userId);

    /**
     * Creates an object containing a list of the topmost users and their scores and positions. Tied users are shown with the same
     * position ordered by their user id. The user with the most points is presented in position one.
     * @param maxUsers The maximum number of users that we want to get listed.
     * @return An object containing all the users and their scores and positions.
     */
    @NonNull
    @CheckReturnValue
    public HighscoresTableData getHighScores(int maxUsers);

    /**
     * Creates an implementation of {@code HighscoresTable} that holds it state in an {@link AtomicReference}.
     * @return An implementation of {@code HighscoresTable}.
     */
    public static HighscoresTable getCasImplementation() {
        return new CasHighscoresTable();
    }

    /**
     * Creates an implementation of {@code HighscoresTable} that holds it state guarded by synchronization.
     * @return An implementation of {@code HighscoresTable}.
     */
    public static HighscoresTable getSynchronizedImplementation() {
        return new SynchronizedHighscoresTable();
    }

    /**
     * Implementation of {@link HighscoresTable} that holds it state in an {@link AtomicReference}.
     * @author Victor Williams Stafusa da Silva
     */
    public static class CasHighscoresTable implements HighscoresTable {
        /**
         * Stores the application state.
         */
        @NonNull
        private final AtomicReference<ApplicationState> state;

        /**
         * Creates a new initially empty {@code HighscoreTable}.
         */
        public CasHighscoresTable() {
            this.state = new AtomicReference<>(new ApplicationState());
        }

        /**
         * {@inheritDoc}
         * @param data {@inheritDoc}
         * @throws IllegalArgumentException {@inheritDoc}
         */
        @Override
        public void addScore(@NonNull UserData data) {
            if (data == null) throw new IllegalArgumentException();
            state.updateAndGet(s -> s.addScore(data));
        }

        /**
         * {@inheritDoc}
         * @param userId {@inheritDoc}
         * @return {@inheritDoc}
         */
        @NonNull
        @Override
        @CheckReturnValue
        public Optional<PositionedUserData> findUser(long userId) {
            return state.get().findUser(userId);
        }

        /**
         * {@inheritDoc}
         * @param maxUsers {@inheritDoc}
         * @return {@inheritDoc}
         */
        @NonNull
        @Override
        @CheckReturnValue
        public HighscoresTableData getHighScores(int maxUsers) {
            return state.get().getHighScores(maxUsers);
        }
    }

    /**
     * Implementation of {@link HighscoresTable} that holds it state guarded by synchronization.
     * @author Victor Williams Stafusa da Silva
     */
    public static class SynchronizedHighscoresTable implements HighscoresTable {
        /**
         * Stores the application state.
         */
        @NonNull
        private ApplicationState state;

        /**
         * Synchronization lock.
         */
        @NonNull
        private final Object lock;

        /**
         * Creates a new initially empty {@code HighscoreTable}.
         */
        public SynchronizedHighscoresTable() {
            this.state = new ApplicationState();
            this.lock = new Object();
        }

        /**
         * {@inheritDoc}
         * @param data {@inheritDoc}
         * @throws IllegalArgumentException {@inheritDoc}
         */
        @Override
        public void addScore(@NonNull UserData data) {
            if (data == null) throw new IllegalArgumentException();
            synchronized (lock) {
                state = state.addScore(data);
            }
        }

        /**
         * {@inheritDoc}
         * @param userId {@inheritDoc}
         * @return {@inheritDoc}
         */
        @NonNull
        @Override
        @CheckReturnValue
        public Optional<PositionedUserData> findUser(long userId) {
            ApplicationState current;
            synchronized (lock) {
                current = state;
            }
            return current.findUser(userId);
        }

        /**
         * {@inheritDoc}
         * @param maxUsers {@inheritDoc}
         * @return {@inheritDoc}
         */
        @NonNull
        @Override
        @CheckReturnValue
        public HighscoresTableData getHighScores(int maxUsers) {
            ApplicationState current;
            synchronized (lock) {
                current = state;
            }
            return current.getHighScores(maxUsers);
        }
    }
}

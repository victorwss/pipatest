package ninja.javahacker.temp.pipatest;

import edu.umd.cs.findbugs.annotations.CheckReturnValue;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.jcip.annotations.Immutable;
import ninja.javahacker.temp.pipatest.avl.ImmutableWeightedAvlTree;
import ninja.javahacker.temp.pipatest.data.HighscoresTableData;
import ninja.javahacker.temp.pipatest.data.PositionedUserData;
import ninja.javahacker.temp.pipatest.data.UserData;

/**
 * Represents a state of the application. That state is maintained in a set of immutable weighted AVL trees. The state itself is
 * immutable and the operation that adds a score to a user actually creates a new state.
 * @author Victor Williams Stafusa da Silva
 */
@Immutable
public final class ApplicationState {

    /**
     * Map the users from the number os points that each one achieved. This is an {@code ImmutableWeightedAvlTree} because we need
     * an immutable class with {@code O(log N)} complexity for search and changes (which actually creates new instances). Since we
     * might have several users with the same number of points, we use a second-level nested {@code ImmutableWeightedAvlTree} for
     * keeping their users' ids.
     *
     * <p>In the internal {@code ImmutableWeightedAvlTree} we have interest in the users' ids with are kept as the keys of the tree.
     * We have no interest in the values themselves kept in the tree, so we use a dummy class for them.</p>
     *
     * <p>Each node in the external {@code ImmutableWeightedAvlTree} has a weight that is the total weight of
     * the internal {@code ImmutableWeightedAvlTree}. Each node in the internal {@code ImmutableWeightedAvlTree} has a weight of 1.</p>
     */
    @NonNull
    private final ImmutableWeightedAvlTree<Long, ImmutableWeightedAvlTree<Long, Dummy>> pointsToUsers;

    /**
     * Map the users from their ids to their number of points. This is an {@code ImmutableWeightedAvlTree} because we need
     * an immutable class with {@code O(log N)} complexity for search and changes (which actually creates new instances).
     */
    @NonNull
    private final ImmutableWeightedAvlTree<Long, Long> usersToPoints;

    /**
     * The dummy class for the internal nodes of the values of the internal trees of the
     * {@link ApplicationState#pointsToUsers pointsToUsers} field.
     * @author Victor Williams Stafusa da Silva
     */
    private static enum Dummy {
        /**
         * Dummy instance.
         */
        DUMMY
    }

    /**
     * Constructor for the initial state of the application, which is empty and features no users.
     */
    public ApplicationState() {
        this.pointsToUsers = new ImmutableWeightedAvlTree<>();
        this.usersToPoints = new ImmutableWeightedAvlTree<>();
    }

    /**
     * Constructor for the non-initial states of the application, which features a lot of users with different scores.
     * @param pointsToUsers The value for the {@link ApplicationState#pointsToUsers pointsToUsers} field.
     * @param usersToPoints The value for the {@link ApplicationState#usersToPoints usersToPoints} field.
     */
    private ApplicationState(
            ImmutableWeightedAvlTree<Long, ImmutableWeightedAvlTree<Long, Dummy>> pointsToUsers,
            ImmutableWeightedAvlTree<Long, Long> usersToPoints)
    {
        this.pointsToUsers = pointsToUsers;
        this.usersToPoints = usersToPoints;
    }

    /**
     * Creates a new instance of {@code ApplicationState} where the given user have collected a few more points.
     * @param data The user data containing the user id and the quantity of points that s/he scored.
     * @return A new state for the application.
     */
    @NonNull
    @CheckReturnValue
    public ApplicationState addScore(@NonNull UserData data) {

        // Starts unwrapping the data.
        long id = data.getUserId();
        long earnedPoints = data.getPoints();

        // Use this variable as sketch for new pointsToUsers.
        ImmutableWeightedAvlTree<Long, ImmutableWeightedAvlTree<Long, Dummy>> newPointsToUsers = pointsToUsers;

        // Find out how many points the given user has and also if s/he even exist in the table so far.
        // Complexity of this step is O(log n).
        Optional<Long> optCurrentPoints = usersToPoints.get(id);
        long currentPoints = optCurrentPoints.orElse(0L);

        // If the user already existed and got zero new points, there is no change after all.
        if (optCurrentPoints.isPresent() && earnedPoints == 0) return this;

        // If the user already existed, we need to delete it from the newPointsToUsers tree first before re-adding it.
        // We don't need to caare bout deleting it from the usersToPoints because the number of points will be replaced anyway.
        if (optCurrentPoints.isPresent()) {

            // Remove it from the internal trees of the sketch of the pointsToUsers.
            // Complexity of this step is two O(log n) operations.
            ImmutableWeightedAvlTree<Long, Dummy> usersWithThatManyPointsOld = newPointsToUsers.get(currentPoints).get();
            usersWithThatManyPointsOld = usersWithThatManyPointsOld.remove(id);

            // If the internal tree degenerated to an empty tree, remove it from the external tree.
            // Otherwise, re-add a new internal tree.
            // Either way, it is an O(log n).
            if (usersWithThatManyPointsOld.isEmpty()) {
                newPointsToUsers = newPointsToUsers.remove(currentPoints);
            } else {
                newPointsToUsers = newPointsToUsers.put(
                        currentPoints,
                        usersWithThatManyPointsOld.getTotalWeight(),
                        usersWithThatManyPointsOld);
            }
        }

        // Find out the internal node of the sketch of the pointsToUsers where the user should be added or create a new tree for that.
        // This have a complexity of O(log n).
        ImmutableWeightedAvlTree<Long, Dummy> usersWithThatManyPointsNew = newPointsToUsers
                .get(currentPoints + earnedPoints)
                .orElse(new ImmutableWeightedAvlTree<>());

        // Then, add the user in the internal tree. Complexity is O(log n).
        usersWithThatManyPointsNew = usersWithThatManyPointsNew.put(id, 1, Dummy.DUMMY);

        // Add the internal tree to the external one. This have a complexity of O(log n).
        newPointsToUsers = newPointsToUsers.put(
                currentPoints + earnedPoints,
                usersWithThatManyPointsNew.getTotalWeight(),
                usersWithThatManyPointsNew);

        // Finally, update the usersToPoints. This have a complexity of O(log n).
        ImmutableWeightedAvlTree<Long, Long> newUsersToPoints = usersToPoints.put(id, 0, currentPoints + earnedPoints);

        // Produce a new state.
        // The total complexity is 8 operations of O(log n) size plus some O(1) operations.
        return new ApplicationState(newPointsToUsers, newUsersToPoints);
    }

    /**
     * Find the score and the position of a user given by his/her id.
     * @param userId The id of the user we want to find out the score and the position.
     * @return An {@link Optional} containing the user data or an empty one if the user had never been seen before.
     */
    @NonNull
    @CheckReturnValue
    public Optional<PositionedUserData> findUser(long userId) {
        return usersToPoints.get(userId).map((points) -> {
            int position = 1 + pointsToUsers.getRightWeight(points).orElseThrow(AssertionError::new);
            return new PositionedUserData(userId, points, position);
        });
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

        /**
         * Stops collecting new users data.
         * @author Victor Williams Stafusa da Silva
         */
        class StopIt extends RuntimeException {

            /** Important for serialization. */
            private static final long serialVersionUID = 1L;
        }

        // We will collect the users' data here. Constructs the list with the proper size.
        List<PositionedUserData> output = new ArrayList<>(Math.max(maxUsers, usersToPoints.getTotalWeight()));

        // Traverse the pointsToUsers tree and add the users' data to the output until it is full.
        try {

            // Iterate the users by points in reverse order. I.E. from the users with most points to the users with less points.
            // The weight of each node is the number of users tied in the node. The weight to the right, how many users with more
            // points and the weight to the left, how many with less points.
            pointsToUsers.forEachReverse((points, tiedUsers, howManyWithLessPoints, howManyTied, howManyWithMorePoints) -> {

                // Each of the internal nodes of the pointsToUsers are a ImmutableWeightedAvlTree ordered by the userId. Iterate
                // it in order to get the users' id.
                tiedUsers.forEach((userId, dummy, shouldBeZeroA, shouldBeZeroB, shouldBeZeroC) -> {

                    // First, check if we are already full.
                    if (output.size() >= maxUsers) throw new StopIt();

                    // We aren't full yet, so add the user's data to the list.
                    output.add(new PositionedUserData(userId, points, howManyWithMorePoints + 1));
                });
            });
        } catch (StopIt e) {
            // Just swallow the exception.
        }

        // Produce the result.
        return new HighscoresTableData(output);
    }
}

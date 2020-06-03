# Highscores game server

In order to build and run the solution, you will need to have:
- Java ≥ 8 properly installed.
- Gradle ≥ 6.

## How to build and run it

- To quickly compile and run it, execute the following command: `gradle clean asemble run`

- After it starts to run, you might access the URL http://localhost:7002/swagger-ui to interact with the API using the Swagger UI.

- If you already compiled it before and don't want to recompile, use just `gradle run`

- To just compile it, execute the following command: `gradle clean assemble`

- To run all the tests on it (JUnit, Checkstyle and SpotBugs), execute the following command: `gradle check tests`<br>WARNING: this may take up several minutes.

- To produce all the Javadoc's documentation, run this: `gradle javadoc`

- If you want to perform all the above tasks in a single run, execute just this: `gradle build javadoc run`

## Notes about the build

The following warning will show up when gradle runs Spot Bugs as a part of the build proccess:

```
The following classes needed for analysis were missing:
  apply
  get
  run
  accept
  handleg 
```

Don't worry with that. Its [a long-standing problem in SpotBugs](https://github.com/spotbugs/spotbugs/issues/6), but it is innofensive. So, you might just ignore that message.

## Notes about the implementation

The implementation uses [Javalin](https://javalin.io/) to serve HTTP requests, which runs on top of an embedded Jetty server.
This allows the server to run as a standalone proccess starting with the plain old traditional `public static void main(String[])`.

The highscore table is kept in a set of immutable weighted AVL trees (see the class `ImmutableWeightedAvlTree`).
Nodes are inserted and removed in the trees by creating modified copies of nodes instead of mutating them.
However, on every change, only a few nodes (`O(log n)`) are changed, while the rest are simply reused.

For comparison, other strategies, such as using `ConcurrentHashMap`, `SconcurrentSkipListMap`, plain `HashMap`, plain `TreeMap`,
synchronized `HashMap` or synchronized `TreeMap` were considered.
None of them iterates the elements in order while isolating traversals from seeing concurrent changes without blocking other threads.
So, this is the main reason why the `ImmutableWeightedAvlTree` was conceived. To see a comparision between them, check out the
`ConcurrencyTest` test class.

Specifically, the highscore table contains two AVL trees names `pointsToUsers` and `usersToPoints` (see the `ApplicationState` class).

The `pointsToUsers` tree is the most complex one. It is a weighted AVL tree with nested (unweighted) AVL trees in each node.
The weighted AVL tree is ordered by the number of points of each user and stores in each node, the users' ids with the
corresponding points in a nested AVL tree. The nested AVL tree consists of an ordered collection of user ids.
The size of the nested trees are the weight of each node on the weighted tree.
The weighted AVL tree also maintains in each node the weight of its subtrees.

The `usersToPoints` AVL tree maps user ids to their points.

So, to add an user, we need to first find it in the `pointsToUsers` AVL tree (and nested trees),
remove it if exists (and grab his/her current number of points) and then re-add it to the tree with his/her new total of points.
Then, we (re)add it in the `usersToPoints` tree.

To find out a user given his/her id, we first look for his/her points in the `usersToPoints` tree
and then find him/her out in the `usersToPoints` tree, calculating the weight to the right of the found user node.
That weight tells how many users have a better position than his/her.
The weight of the node (a nestes AVL tree) tells how many users are tied with that many points.

To make out the highscore table, the `pointsToUsers` tree is traversed in reverse order.
The weight of the nodes is used to compute the user's position.

Finally, in order to allow the highscores table to change, the `HighscoresTable` interface features two implementation,
each one with a single reference to an instance of the `ApplicationState` class.
Changing the table is just a matter to change that reference.
The two implementations are named `CasHighscoresTable` and `SynchronizedHighscoresTable`.
The `CasHighscoresTable` uses an `AtomicReference` to guard the reference to the `ApplicationState`,
while the `SynchronizedHighscoresTable` uses synchronization.
In both cases, the operation of traversing the tree or finding out a user need a very brief access to the
guarded variable in order to be able to get the reference to the `ApplicationState`.
Adding a user is more complicated, and needs to either keep the synchronization lock longer (as in `SynchronizedHighscoresTable`)
or possible many retries (performed intrenally by `AtomicReference`'s implementation) due to thread competition. The implementation
actually used was determined through a stress test (see the method `testHeavyUse` in the test class `HighscoresTableTest`).
The test showed up that `SynchronizedHighscoresTable` is faster in scenarios of stress.

# Highscores game server

In order to build and run the solution, you will need to have:
- Java ≥ 8 properly installed.
- Gradle ≥ 6.

## How to build and run it

- First, check-out the project into a folder.

- To quickly compile and run it, execute the following command in the folder where the project was checked out: `gradle clean asemble run`

- After it starts to run, you might access the URL http://localhost:7002/swagger-ui to interact with the API using the Swagger UI. This also allows you to actually interact with the API. Further, this exposes the URLs used for direct interaction.

- If you want to run it, have already compiled it before and don't want to recompile, use just the follwing command: `gradle run`

- To just compile it, without running, execute the following command: `gradle clean assemble`

- To compile and execute all the tests on it (JUnit, Checkstyle and SpotBugs), execute the following command: `gradle build`<br>WARNING: this may take up several minutes, mainly due to a performance test (see below).

- To produce all the Javadoc's documentation, run this: `gradle javadoc`<br>NOTE: After it is generated, you might find it out the javadocs in the `/build/docs/javadoc/` subfolder, inside the folder where the project was checked out.

- If you want to perform all the above tasks in a single run, execute just this: `gradle build javadoc run`<br>WARNING: This may take up several minutes because it includes the aforementioned tests.

## Notes about the build

The following warning will show up when gradle runs SpotBugs as a part of the build proccess:

```
The following classes needed for analysis were missing:
  apply
  get
  run
  accept
  handle
```

Don't worry with that. Its [a long-standing problem in SpotBugs](https://github.com/spotbugs/spotbugs/issues/6), but it is innofensive. So, just ignore that message.

## Notes about the implementation

### Components used

The implementation uses [Javalin](https://javalin.io/) to serve HTTP requests, which runs on top of an embedded [Jetty](https://www.eclipse.org/jetty/) server.
This allows the server to run as a standalone proccess starting with the good old plain `public static void main(String[])`, with no need to worry about configuring and executing a servlet container and having to deploy the application.

Other than Javalin, this application also uses [Jackson](https://github.com/FasterXML/jackson) for serializing and deserializing objects as JSON. SLF4j is used as a dependency for those. Javalin also features [an OpenAPI/Swagger plugin](https://javalin.io/plugins/openapi) that was also used with this implementtion.

As part of the build proccess, to ensure its quality, some [JUnit 5](https://junit.org/junit5/) tests were also developed. Further, [Checkstyle](https://checkstyle.sourceforge.io/) and [SpotBugs](https://spotbugs.github.io/) were also used to ensure to the best extent possible that no bugs or bad practices slip out during the development proccess.

### Specialized data structures

The highscore table is kept in a set of immutable weighted AVL trees (see the class `ImmutableWeightedAvlTree`).
Nodes are inserted and removed in the trees by creating modified copies of nodes instead of mutating them.
However, on every change, only a few nodes (`O(log n)`) are changed, while the rest are simply reused.

For comparison, other strategies, such as using `ConcurrentHashMap`, `ConcurrentSkipListMap`, plain `HashMap`, plain `TreeMap`,
synchronized `HashMap` or synchronized `TreeMap` were considered.
None of them iterates the elements in order while isolating traversals from seeing concurrent changes without blocking other threads.
So, this is the main reason why the `ImmutableWeightedAvlTree` was conceived. To see a comparision between them, check out the
`ConcurrencyTest` test class.

### Internal data organization

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

The `ApplicationState` class itself is immutable, so creating a new instance for each mutation operation (the only one is to add a user or update his/her score) actually creates a new instance of that class. This proccess tries to reuse the most nodes as possible inside the AVL trees contained in the `ApplicationState` class to ensure that the operation has an `O(log n)` complexity (although with somewhat considerable constant factors). 

### Thread-safety and concurrency

In order to allow the highscores table to change, the `HighscoresTable` interface features two implementation,
each one with a single reference to an instance of the `ApplicationState` class. This is the only place where an actual mutation happens (instead of creting a new instance based on an older one). Changing the table is just a matter to change that reference.

The two implementations of the `HighscoresTable` interfaace are named `CasHighscoresTable` and `SynchronizedHighscoresTable`.
The `CasHighscoresTable` uses an `AtomicReference` to guard the reference to the `ApplicationState`,
while the `SynchronizedHighscoresTable` uses synchronization.

In both cases, the operation of traversing the tree or finding out a user need a very brief access to the
guarded variable in order to be able to get the reference to the `ApplicationState`.
Adding a user is more complicated, and needs to either keep the synchronization lock for the duration of all the operation (as in `SynchronizedHighscoresTable`)
or possible perform many retries due to thread competition (this is what the `AtomicReference` claass do internally).

To find out which implementation would be the faster, a performance test (see the method `testHeavyUse` in the test class `HighscoresTableTest`) was designed, creating a large number of threads performing a large operations each at the same time.
The test showed up that `SynchronizedHighscoresTable` is faster in scenarios of stress, so that was the one used.

### Servicing HTTP

Finally, the main class of the application is (unsurpisingly) called `Main`. The class thaat actually uses Javalin in order to serve HTTP requests is the `GameServer` class, which also is responsible for configuring the OpenAPI/Swagger plugin for Javalin.

The input and output data are serialized and deserialized by Jackson, which maps them to three different classes, namely `UserData`, `PositionedUserData` and `HighscoresTableData`, accordingly to the data format needed by each of the application's endpoint. Those classes are simple immutable carrier of data with no business logic.

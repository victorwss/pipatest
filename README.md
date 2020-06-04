# Highscores game server

Welcome to the highscores game server. This is a HTTP-based mini game back-end developed in Java which registers score points for different users, with the capability to return the current user position and high score list.

## Building and running the application

### Requirements

In order to build and run the solution, you will need to have:

- Java ≥ 8 properly installed.

- Gradle ≥ 6 properly installed.

### How to build it

1. Check-out the project into a folder.

2. Quickly compile it, by executing the following command in the folder where the project was checked out: `gradle clean assemble fatJar`

3. Grab the `Pipatest-all-1.0.jar` file inside the `/build/libs` subfolder of the folder where the project was checked out and copy that file to wherever you want to.

### How to run it

4. Run the `Pipatest-all-1.0.jar` file (see #3 above) with the `java -jar Pipatest-all-1.0.jar` command. Or perhaps, just click twice the file's icon and your OS might run it.

5. After it starts to run, access the URL http://localhost:7002/swagger-ui and notice that the Swagger UI screen loads, allowing you to interact with the API.

6. Use the Swagger UI interface to perform actions in the exposed API. Notice that the Swagger UI exposes the URLs from the underlying API, so you might use that API directly instead later without needing to rely on Swagger UI if you want to.

### Producing Javadocs

- To produce all the Javadoc's documentation, run this: `gradle javadoc`<br>NOTE: After it is generated, you might find it out the javadocs in the `/build/docs/javadoc/` subfolder, inside the folder where the project was checked out.

- To package the javadocs inside a JAR file, use this command: `gradle javadocJar`<br>Then, you will be able to locate the JAR file `Pipatest-javadoc-1.0.jar` inside the `/build/libs` subfolder of the folder where the project was checked out.

### Further options about building

- To clean up everything from the `/build` subdirectory in order to perform a fresh start, run the command `gradle clean` - or, simply just delete that subfolder altogether.

- For running the application through Gradle without needing to produce a JAR, run the following command: `gradle run`

- To compile and execute all the unit tests on it (JUnit, Checkstyle and SpotBugs), execute the following command: `gradle build`<br>To see the generated reports by those tool, take a look inside the `/build/reports` subfolder within the folder where the project was checked out.

- To execute performance tests, execute the following command: `gradle build`<br>WARNING: Those tests are very CPU-intensive and might take several minutes to finish. See below the reasoning for that.

- To package the source files inside a JAR file, use this command: `gradle sourcesJar`<br>Then, you will be able to locate the JAR file `Pipatest-sources-1.0.jar` inside the `/build/libs` subfolder of the folder where the project was checked out.

### Notes about the build

The following warning will show up when Gradle runs SpotBugs as a part of the build proccess:

```
The following classes needed for analysis were missing:
  apply
  get
  run
  accept
  handle
```

Don't worry with that. Its [a long-standing problem in SpotBugs](https://github.com/spotbugs/spotbugs/issues/6), but it is inoffensive. So, just ignore that message.

## Notes about the implementation

### Components used

The implementation uses [Javalin](https://javalin.io/) to serve HTTP requests, which runs on top of an embedded [Jetty](https://www.eclipse.org/jetty/) server.
This allows the server to run as a standalone proccess starting with the good old plain `public static void main(String[])`, with no need to worry about configuring and executing a servlet container and having to deploy the application.

Other than Javalin, this application also uses [Jackson](https://github.com/FasterXML/jackson) for serializing and deserializing objects as JSON. SLF4J is used as a dependency for those. Javalin also features [an OpenAPI/Swagger plugin](https://javalin.io/plugins/openapi) that was also used with this implementtion.

As part of the build proccess, to ensure its quality, some [JUnit 5](https://junit.org/junit5/) tests were also developed. Further, [Checkstyle](https://checkstyle.sourceforge.io/) and [SpotBugs](https://spotbugs.github.io/) were also used to ensure to the best extent possible that no bugs or bad practices slips out during the development proccess.

### Specialized data structures

The highscore table is kept in a set of immutable weighted AVL trees (see the class `ImmutableWeightedAvlTree`).
Nodes are inserted and removed in the trees by creating modified copies of nodes instead of mutating them.
However, on every change, only a few nodes (`O(log n)`) are changed, while the rest are simply reused. In fact, just the directly changed nodes and its ancestors are re-created, with all the remaining nodes being reused.

In those AVL trees, each node features a key, a value and a weight. They are self-balancing binary search trees sorted by keys. So, it could be used as a `Map` implementation, if it weren't for the fact that it was designed to be immutable mappings that creates entire new instances for each modification while the JDK's `Map` interface, on the other hand, isn't really suitable for that. 

The AVL trees are weighted in a way that each node can have a different weight. Also, each node stores the weight of its left-subtree and its right-subtree. This way, by knowing those three weights for a node and its ancestor nodes, it is possible to calculate the total weight of the tree for both all the nodes to the left and all the nodes to the right. This is useful because the users' positions are calculated as subtree weights and when we are going to find out any particular node inside the tree, we will also necessarily visit its ancestor nodes, so being able to calculate how many users are either to the left or the right (or tied) to the searched one.

For comparison, other strategies, such as using `ConcurrentHashMap`, `ConcurrentSkipListMap`, plain `HashMap`, plain `TreeMap`,
synchronized `HashMap` or synchronized `TreeMap` were considered.
None of them iterates the elements in order while isolating traversals from seeing concurrent changes without blocking other threads.
So, this is the main reason why the `ImmutableWeightedAvlTree` was conceived. To see a comparision between them, check out the
`ConcurrencyTest` test class.

### Internal data organization

Specifically, the highscore table contains two AVL trees named `pointsToUsers` and `usersToPoints` (see the `ApplicationState` class).

The `ApplicationState` class itself is immutable, so creating a new instance for each mutation operation (the only one is to add a user or update his/her score) actually creates a new instance of that class. This proccess tries to reuse the most nodes as possible inside the AVL trees contained in the `ApplicationState` class to ensure that the operation has an `O(log n)` complexity (although with somewhat considerable constant factors). 

The `pointsToUsers` tree is the most complicated one among those two. Those are the rules governing its behaviour:

- It is a weighted AVL tree with nested (unweighted) AVL trees in each node.

- The weighted AVL tree uses number of points as a key (being ordered by them) and stores as a value in each node, the users' ids with the corresponding points in a nested AVL tree.

- The nested AVL tree consists of an ordered collection of user ids as keys, and since there would be no interested in the values, they are just dummy values.

- The size of the nested trees are the weight of each node on the big weighted AVL tree. This represents how many users are tied with the same number of points.

- The weighted AVL tree also maintains in each node, the weight of their corresponding subtrees.

The `usersToPoints` AVL tree however, is conceptually much simpler. It just maps user ids to the number of their points.

So, to add an user, we need to:

1. Find out how many points the user currently have given his/her id. For that, we just consult the `usersToPoints` AVL tree.

2. If the user exists in the `usersToPoints` AVL tree and is not earning zero points (in that case we simply exit doing nothing), we need to remove it from the `pointsToUsers` AVL tree, since they would be now mispositioned. So, we first find out which is the internal AVL tree where the user was before, by searching the `pointsToUsers` tree using the old user's score as a key.

3. After having the internal tree, we removes the user id from it (which actually creates a new internal tree) and re-adds it to the same place where it was (which creates a new `pointsToUsers` tree). If the internal trees degenerates to an empty tree, instead of re-adding it, we just remove the old one. The proccess of adding or removing nodes in the weight tree will also naturally recalculate the weights.

4. Now, we find out by new number of points the user have, which is the internal node where his/her user id should be added. If there is no such internal tree, creates a new one. 

5. After finding out the internal tree, adds the user id to it (which actually creates a new internal tree) and re-adds it back to `pointsToUsers` (which creates a new `pointsToUsers`).

6. Finally, add the new score of the user to the `usersToPoints` tree (again, in truth, this creates an entire new tree). This will naturally replace the old score with the new one.

7. Having the newly constructed references for the `pointsToUsers` and `usersToPoints` trees, creates a new `ApplicationState` instance with that.

To find out the score and the position of an user given his/her id, we look at the `usersToPoints` tree to find out how many points does s/he have and then look out in the `pointsToUsers` which internal tree corresponds to that number of points. From the weights of the tree, we would also be able to tell the weight of the nodes to the right and to the left of the internal tree (as the weight of the internal tree itself). The weights to the left tell how many other users have a better score, so we just add one to this number in order to calculate his/her positions. Note that all the users in the same internal tree are tied in the score.

To make out the highscore table, the `pointsToUsers` tree is traversed in reverse order. The reason for that is because we would start with the users with the most points and go down to the users with less points. The weights are used to track the user position, although we could easily do that without them here (however, they are important for finding out the position of a single user without having to traverse the tree, so we can't get rid of them). Further the internal trees, which represents users tied with the same score and some position, is traversed in order.

### Thread-safety and concurrency

In order to allow the highscores table to change, the `HighscoresTable` interface features two implementation,
each one with a single reference to an instance of the `ApplicationState` class. This is the only place where an actual mutation happens (instead of creating a new instance based on an older one). Changing the table is just a matter to change that reference.

The two implementations of the `HighscoresTable` interface are named `CasHighscoresTable` and `SynchronizedHighscoresTable`.
The `CasHighscoresTable` uses an `AtomicReference` to guard the reference to the `ApplicationState`,
while the `SynchronizedHighscoresTable` uses synchronization.

In both cases, the operation of traversing the tree or finding out a user need a very brief access to the guarded variable in order to be able to get the reference to the `ApplicationState`.
Adding a user is more complicated, and needs to either keep the synchronization lock for the duration of all the operation (as in `SynchronizedHighscoresTable`) or possible perform many retries due to thread competition (this is what the `AtomicReference` class do internally).

To find out which implementation would be the faster, a performance test (see the `HighscoresTablePerformanceTest` test class) was designed, creating a large number of threads running at the same time and performing a large number of operations in each thread.
The test showed up that the one stressing `CasHighscoresTable` took roughly 2 minutes and 45 seconds to finish while the one stressing `SynchronizedHighscoresTable` took roughy 2 minutes and 20 seconds. Of course, several runs will give varying results, but the proportion between running times was always roughly the same. So, the implementation done with `SynchronizedHighscoresTable` was choosen to be actually used and acquired in the `GameServer` class. You might re-run this test with the `gradle performanceTest` command, but be warned that they are very CPU-intensive and may take several minutes to finish.

### Servicing HTTP

Finally, the main class of the application is (unsurprisingly) called `Main`. The class that actually uses Javalin in order to serve HTTP requests is the `GameServer` class, which also is responsible for configuring the OpenAPI/Swagger plugin for Javalin.

The input and output data are serialized and deserialized by Jackson, which maps them to three different classes, namely `UserData`, `PositionedUserData` and `HighscoresTableData`, accordingly to the data format needed by each of the application's endpoint. Those classes are simple immutable carrier of data with no business logic.

Finally there is a `FunctionUtils` class which contains a few helper methods that didn't fit anywhere else and are used only be the `GameServer` class. However, they are not part of it because, although it is not reused anywhere else, they were designed to bee reusable and keeping them in the `GameServer` would hurt its cohesiveness.

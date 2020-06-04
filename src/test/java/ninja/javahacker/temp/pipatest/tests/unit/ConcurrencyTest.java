package ninja.javahacker.temp.pipatest.tests.unit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import ninja.javahacker.temp.pipatest.avl.ImmutableWeightedAvlTree;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * This tests compares the implementations of several data structures and proves that only the {@link ImmutableWeightedAvlTree}
 * implementation that is ordered and is suitable for traversals that don't see concurrent data changes without blocking them.
 *
 * <p>The implementations are:</p>
 * <ol>
 * <li>{@link ImmutableWeightedAvlTree}</li>
 * <li>{@link ConcurrentHashMap}</li>
 * <li>{@link ConcurrentSkipListMap}</li>
 * <li>{@link TreeMap}</li>
 * <li>{@link HashMap}</li>
 * <li>synchronized {@link TreeMap}</li>
 * <li>synchronized {@link HashMap}</li>
 * </ol>
 *
 * <p>This tests only behavioral correctness. This is not a performance test.</p>
 *
 * @author Victor Williams Stafusa da Silva
 */
public class ConcurrencyTest {

    /**
     * Used to define the number of elements in each implementation to test.
     */
    private static final int MAX = 100;

    /**
     * Specifies what are the operations that we need to perform in our implementations.
     */
    private static interface ImplementationOperations {
        public void put(String key, Integer value);

        public void forEach(BiConsumer<String, Integer> toDo);

        public void remove(String key);

        @Override
        public String toString();

        public Optional<Integer> get(String key);
    }

    /**
     * Adapt the {@link Map}-based implementation to our specification.
     */
    private static final class MapAdapter implements ImplementationOperations {
        private final Map<String, Integer> delegate;

        public MapAdapter(Map<String, Integer> delegate) {
            this.delegate = delegate;
        }

        @Override
        public Optional<Integer> get(String key) {
            return Optional.ofNullable(delegate.get(key));
        }

        @Override
        public void put(String key, Integer value) {
            delegate.put(key, value);
        }

        @Override
        public void forEach(BiConsumer<String, Integer> toDo) {
            delegate.forEach(toDo);
        }

        @Override
        public void remove(String key) {
            delegate.remove(key);
        }
    }

    /**
     * Adapt the {@link ImmutableWeightedAvlTree}-based implementation to our specification.
     */
    private static final class AvlTreeAdapter implements ImplementationOperations {
        private final AtomicReference<ImmutableWeightedAvlTree<String, Integer>> delegate;

        public AvlTreeAdapter() {
            this.delegate = new AtomicReference<>(new ImmutableWeightedAvlTree<>());
        }

        @Override
        public Optional<Integer> get(String key) {
            return delegate.get().get(key);
        }

        @Override
        public void put(String key, Integer value) {
            delegate.updateAndGet(x -> x.put(key, 1, value));
        }

        @Override
        public void forEach(BiConsumer<String, Integer> toDo) {
            delegate.get().forEach((x, y, lw, nw, rw) -> toDo.accept(x, y));
        }

        @Override
        public void remove(String key) {
            delegate.updateAndGet(x -> x.remove(key));
        }
    }

    /**
     * Our six implementations.
     */
    private enum ImplementationChoice {
        AVL_TREE(true, true, "ok",
                () -> new AvlTreeAdapter()),
        CONCURRENT_SKIP_LIST_MAP(true, true, "fail",
                () -> new MapAdapter(new ConcurrentSkipListMap<>())),
        CONCURRENT_HASH_MAP(false, true, "fail",
                () -> new MapAdapter(new ConcurrentHashMap<>(MAX))),
        TREE_MAP(true, true, "ConcurrentModificationException",
                () -> new MapAdapter(new TreeMap<>())),
        SYNCHRONIZED_TREE_MAP(true, false, "ConcurrentModificationException",
                () -> new MapAdapter(Collections.synchronizedMap(new TreeMap<>()))),
        SYNCHRONIZED_HASH_MAP(false, false, "fail",
                () -> new MapAdapter(Collections.synchronizedMap(new HashMap<>(MAX))));

        private final boolean shouldWorkInIterationOrder;
        private final boolean shouldWorkInSimultaneousTraversals;
        private final String shouldGoWrongAtIsolation;
        private final Supplier<ImplementationOperations> factory;

        private ImplementationChoice(
                boolean shouldWorkInIterationOrder,
                boolean shouldWorkInSimultaneousTraversals,
                String shouldGoWrongAtIsolation,
                Supplier<ImplementationOperations> factory)
        {
            this.shouldWorkInIterationOrder = shouldWorkInIterationOrder;
            this.shouldWorkInSimultaneousTraversals = shouldWorkInSimultaneousTraversals;
            this.shouldGoWrongAtIsolation = shouldGoWrongAtIsolation;
            this.factory = factory;
        }
    }

    /**
     * Test sole constructor.
     */
    public ConcurrencyTest() {
    }

    /**
     * Test if the implementation iterates its elements ordered.
     * @param choice The implementation to test.
     */
    @ParameterizedTest(name = "{displayName}[{argumentsWithNames}]")
    @EnumSource(ImplementationChoice.class)
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testIterationOrder(ImplementationChoice choice) {

        // Gets an instance of the implementation.
        ImplementationOperations impl = choice.factory.get();

        // Populate the implementation with many different elements in an way that many implementations would need to perform
        // a lot of operations to internally reorganize their data.
        int prime = 43;
        for (int i = 0; i < MAX; i++) {
            if (i % 3 == 0) continue;
            impl.put("a" + (i * prime) % MAX, 1000 + i);
        }
        for (int i = 1; i < MAX; i += 3) {
            impl.remove("a" + (i * prime) % MAX);
        }
        for (int i = 0; i < MAX; i += 3) {
            impl.put("a" + (i * prime) % MAX, 2000 + i);
        }
        for (int i = 1; i < MAX; i += 3) {
            impl.put("a" + (i * prime) % MAX, 3000 + i);
        }

        // Those lists are ued to verify the iteration order.
        List<String> in1 = new ArrayList<>(MAX);
        List<String> in2 = new ArrayList<>(MAX);

        // Iterate the data.
        impl.forEach((x, y) -> {
            in1.add(x);
            in2.add(x);
        });

        // Check the result.
        in1.sort(String::compareTo);
        if (choice.shouldWorkInIterationOrder) {
            Assertions.assertEquals(in1, in2);
        } else {
            Assertions.assertNotEquals(in1, in2);
        }
    }

    /**
     * Test if the implementation isolates traversals from seeing concurrent changes.
     * @param choice The implementation to test.
     */
    @ParameterizedTest(name = "{displayName}[{argumentsWithNames}]")
    @EnumSource(ImplementationChoice.class)
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testIsolation(ImplementationChoice choice) {

        // Gets an instance of the implementation.
        ImplementationOperations impl = choice.factory.get();

        // Populates the implementation with some data with some unpredictable structure.
        int prime = 13;
        for (int i = 0; i < MAX; i++) {
            impl.put("a" + (i * prime) % MAX, i);
        }

        // Some variables for controlling stuff during the traversal.
        String changedKey = "a50";
        String removedKey = "a60";
        String addedKey = "trash";
        AtomicBoolean foundRemoved = new AtomicBoolean(false);
        AtomicInteger count = new AtomicInteger(0);

        // Sanity check.
        Assertions.assertTrue(impl.get(changedKey).isPresent());
        Assertions.assertTrue(impl.get(removedKey).isPresent());
        Assertions.assertFalse(impl.get(addedKey).isPresent());

        /**
         * Used to stop the traversal early.
         */
        class StopIt extends RuntimeException {
            private static final long serialVersionUID = 1L;
        }

        try {
            // Do the traversal.
            impl.forEach((x, y) -> {
                int c = count.incrementAndGet();
                // After the traversal started, do some concurrent screw-ups on them: an addition, an exclusion and an modification.
                if (c == 3) {
                    impl.put(changedKey, 99999);
                    impl.remove(removedKey);
                    impl.put(addedKey, 88888);
                }

                // Check if we saw something that was concurrently removed.
                if (x.equals(removedKey)) foundRemoved.set(true);

                // Check if the concurrent additions and modifications are visible.
                if (addedKey.equals(x) || y.equals(99999) || y.equals(88888)) throw new StopIt();
            });

            // The traversal finished. Check if the screw-ups are commited and if the traversal saw them..
            Assertions.assertEquals(Integer.valueOf(99999), impl.get(changedKey).get());
            Assertions.assertEquals(Integer.valueOf(88888), impl.get(addedKey).get());
            Assertions.assertTrue(!impl.get(removedKey).isPresent());

            // Check if the traversal saw what was concurrently deleted (it should).
            Assertions.assertEquals("ok", choice.shouldGoWrongAtIsolation);
            Assertions.assertTrue(foundRemoved.get());
        } catch (ConcurrentModificationException e) {
            Assertions.assertEquals("ConcurrentModificationException", choice.shouldGoWrongAtIsolation);
        } catch (StopIt e) {
            Assertions.assertEquals("fail", choice.shouldGoWrongAtIsolation);
        }
    }

    /**
     * Test if the implementation can be traverse by multiple threads simultaneously.
     * @param choice The implementation to test.
     */
    @ParameterizedTest(name = "{displayName}[{argumentsWithNames}]")
    @EnumSource(ImplementationChoice.class)
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testSimultaneousIteration(ImplementationChoice choice) throws InterruptedException {

        // Gets an instance of the implementation and populate it with a single entry.
        ImplementationOperations impl = choice.factory.get();
        impl.put("aaa", 3);

        // Creates two threads with a Runnable that checks if both threads can reach the same CyclicBarrier inside the traversal
        // simultaneously. If the implementation makes one of them blocks the other, they would unable to do so. Also, uses the a
        // variable to be able to differentiate their terminating graciously from terminating through an interrupt.
        CyclicBarrier c = new CyclicBarrier(2);
        AtomicBoolean a = new AtomicBoolean(false);
        Runnable r = () -> {
            impl.forEach((x, y) -> {
                try {
                    c.await();
                } catch (InterruptedException | BrokenBarrierException e) {
                    a.set(true);
                }
            });
        };
        Thread t1 = new Thread(r);
        Thread t2 = new Thread(r);
        t1.start();
        t2.start();

        // Wait enough time to be sure that the threads either finished or locked up theirselves.
        Thread.sleep(2000);

        // Check if everything was the expected result. If the threads are locked up, interrupt them both.
        boolean shouldWork = choice.shouldWorkInSimultaneousTraversals;
        if (t1.isAlive() || t2.isAlive()) {
            t1.interrupt();
            t2.interrupt();
            Assertions.assertFalse(shouldWork);
        } else {
            Assertions.assertTrue(shouldWork);
            Assertions.assertFalse(a.get());
        }
    }
}

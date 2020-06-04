package ninja.javahacker.temp.pipatest.tests.performance;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import ninja.javahacker.temp.pipatest.HighscoresTable;
import ninja.javahacker.temp.pipatest.data.UserData;
import ninja.javahacker.temp.pipatest.tests.HighscoresTableImplementation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Performance/stress tests for the {@link HighscoresTable} implementations.
 * @author Victor Williams Stafusa da Silva
 */
public class HighscoresTablePerformanceTest {

    /**
     * Test sole constructor.
     */
    public HighscoresTablePerformanceTest() {
    }

    /**
     * A heavy performance test to measure the time took be a {@link HighscoresTable} under stress.
     * @param choice An instance of {@link HighscoresTableImplementation} that provides an implementation to the {@link HighscoresTable}
     *     interface.
     */
    @ParameterizedTest(name = "{displayName}[{argumentsWithNames}]")
    @EnumSource(HighscoresTableImplementation.class)
    @Timeout(value = 300, unit = TimeUnit.SECONDS)
    void testHeavyUse(HighscoresTableImplementation choice) {
        HighscoresTable ht = choice.createTable();
        int numThreads = 50;
        int operationsPerThread = 50_000;
        CyclicBarrier c = new CyclicBarrier(numThreads + 1);
        Runnable r = () -> {
            try {
                c.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                return;
            }
            for (int i = 0; i < operationsPerThread; i++) {
                if (Thread.interrupted()) return;
                ht.addScore(new UserData(i % 25_000, (i * 271) % 50));
                if (i % 20 == 0) ht.getHighScores(20_000);
                if (i % 5 == 0) ht.findUser(i % 25_000);
            }
        };
        List<Thread> threads = new ArrayList<>(numThreads);
        for (int i = 0; i < numThreads; i++) {
            Thread t = new Thread(r);
            threads.add(t);
            t.start();
        }
        try {
            c.await();
            long time1 = System.nanoTime();
            for (Thread t : threads) {
                t.join();
            }
            long time2 = System.nanoTime();
            System.out.println("Time of " + choice.name() + ": " + (time2 - time1) + " nanos.");
        } catch (InterruptedException | BrokenBarrierException e) {
            for (Thread t : threads) {
                t.interrupt();
            }
            Assertions.fail();
        }
    }
}

package ninja.javahacker.temp.pipatest.tests;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import ninja.javahacker.temp.pipatest.HighscoresTable;
import ninja.javahacker.temp.pipatest.data.HighscoresTableData;
import ninja.javahacker.temp.pipatest.data.PositionedUserData;
import ninja.javahacker.temp.pipatest.data.UserData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * @author Victor Williams Stafusa da Silva
 */
public class HighscoresTableTest {

    /**
     * Our two implementations.
     */
    private enum ImplementationChoice {
        CAS(HighscoresTable::getCasImplementation),
        SYNC(HighscoresTable::getSynchronizedImplementation);

        private final Supplier<HighscoresTable> factory;

        private ImplementationChoice(Supplier<HighscoresTable> factory) {
            this.factory = factory;
        }
    }

    @ParameterizedTest(name = "{displayName}[{argumentsWithNames}]")
    @EnumSource(ImplementationChoice.class)
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testSimpleUse(ImplementationChoice choice) {
        HighscoresTable ht = choice.factory.get();
        ht.addScore(new UserData(555, 70));
        ht.addScore(new UserData(777, 80));
        ht.addScore(new UserData(555, 90));
        ht.addScore(new UserData(888, 80));
        ht.addScore(new UserData(333, 20));

        List<PositionedUserData> desiredList = new ArrayList<>(5);
        desiredList.add(new PositionedUserData(555, 160, 1));
        desiredList.add(new PositionedUserData(777, 80, 2));
        desiredList.add(new PositionedUserData(888, 80, 2));
        desiredList.add(new PositionedUserData(333, 20, 4));
        HighscoresTableData desired = new HighscoresTableData(desiredList);
        Assertions.assertEquals(desired, ht.getHighScores(1000));

        for (PositionedUserData p : desiredList) {
            Assertions.assertEquals(p, ht.findUser(p.getUserId()).get());
        }
        Assertions.assertFalse(ht.findUser(9999).isPresent());
    }

    @ParameterizedTest(name = "{displayName}[{argumentsWithNames}]")
    @EnumSource(ImplementationChoice.class)
    @Timeout(value = 300, unit = TimeUnit.SECONDS)
    void testHeavyUse(ImplementationChoice choice) {
        HighscoresTable ht = choice.factory.get();
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

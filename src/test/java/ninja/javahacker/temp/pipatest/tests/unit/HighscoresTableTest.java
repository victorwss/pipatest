package ninja.javahacker.temp.pipatest.tests.unit;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import ninja.javahacker.temp.pipatest.HighscoresTable;
import ninja.javahacker.temp.pipatest.data.HighscoresTableData;
import ninja.javahacker.temp.pipatest.data.PositionedUserData;
import ninja.javahacker.temp.pipatest.data.UserData;
import ninja.javahacker.temp.pipatest.tests.HighscoresTableImplementation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Unit tests for the {@link HighscoresTable} implementations.
 * @author Victor Williams Stafusa da Silva
 */
public class HighscoresTableTest {

    /**
     * Test sole constructor.
     */
    public HighscoresTableTest() {
    }

    /**
     * A simple test to check the correctness of the given {@link HighscoresTable} implementations.
     * @param choice An instance of {@link HighscoresTableImplementation} that provides an implementation to the {@link HighscoresTable}
     *     interface.
     */
    @ParameterizedTest(name = "{displayName}[{argumentsWithNames}]")
    @EnumSource(HighscoresTableImplementation.class)
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testSimpleUse(HighscoresTableImplementation choice) {
        HighscoresTable ht = choice.createTable();
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
}

package ninja.javahacker.temp.pipatest.tests;

import java.util.ArrayList;
import java.util.List;
import ninja.javahacker.temp.pipatest.HighscoresTable;
import ninja.javahacker.temp.pipatest.data.HighscoresTableData;
import ninja.javahacker.temp.pipatest.data.PositionedUserData;
import ninja.javahacker.temp.pipatest.data.UserData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Victor Williams Stafusa da Silva
 */
public class HighscoresTableTest {

    @Test
    public void testSimpleUse() {
        HighscoresTable ht = new HighscoresTable();
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

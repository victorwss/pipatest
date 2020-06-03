package ninja.javahacker.temp.pipatest.tests;

import java.util.ArrayList;
import java.util.List;
import ninja.javahacker.temp.pipatest.data.HighscoresTableData;
import ninja.javahacker.temp.pipatest.data.PositionedUserData;
import ninja.javahacker.temp.pipatest.data.UserData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Victor Williams Stafusa da Silva
 */
public class DataTests {
    @Test
    public void testUserData() {
        UserData a = new UserData(123, 456);
        Assertions.assertEquals(123, a.getUserId());
        Assertions.assertEquals(456, a.getPoints());
        Assertions.assertEquals("{\"userId\":123,\"points\":456}", a.toString());
        Assertions.assertFalse(a.equals(null));
        Assertions.assertFalse(a.equals("foo"));

        UserData b = new UserData(789, 987);
        Assertions.assertEquals(789, b.getUserId());
        Assertions.assertEquals(987, b.getPoints());
        Assertions.assertEquals("{\"userId\":789,\"points\":987}", b.toString());

        UserData c = new UserData(123, 456);
        Assertions.assertTrue(a.equals(c));
        Assertions.assertFalse(a.equals(b));
        Assertions.assertEquals(a.hashCode(), c.hashCode());
        Assertions.assertNotEquals(a.hashCode(), b.hashCode());
        Assertions.assertEquals(a.toString(), c.toString());
        Assertions.assertNotEquals(a.toString(), b.toString());
    }

    @Test
    public void testUserDataBadInstantiation() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new UserData(-1, 456));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new UserData(5, -1));
        new UserData(5, 0);
        new UserData(0, 5);
    }

    @Test
    public void testPositionedUserData() {
        PositionedUserData a = new PositionedUserData(123, 456, 3);
        Assertions.assertEquals(123, a.getUserId());
        Assertions.assertEquals(456, a.getPoints());
        Assertions.assertEquals(3, a.getPosition());
        Assertions.assertEquals("{\"userId\":123,\"points\":456,\"position\":3}", a.toString());
        Assertions.assertFalse(a.equals(null));
        Assertions.assertFalse(a.equals("foo"));

        PositionedUserData b = new PositionedUserData(789, 987, 5);
        Assertions.assertEquals(789, b.getUserId());
        Assertions.assertEquals(987, b.getPoints());
        Assertions.assertEquals(5, b.getPosition());
        Assertions.assertEquals("{\"userId\":789,\"points\":987,\"position\":5}", b.toString());

        PositionedUserData c = new PositionedUserData(123, 456, 3);
        Assertions.assertTrue(a.equals(c));
        Assertions.assertFalse(a.equals(b));
        Assertions.assertEquals(a.hashCode(), c.hashCode());
        Assertions.assertNotEquals(a.hashCode(), b.hashCode());
        Assertions.assertEquals(a.toString(), c.toString());
        Assertions.assertNotEquals(a.toString(), b.toString());
    }

    @Test
    public void testPositionedUserDataBadInstantiation() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new PositionedUserData(-1, 456, 3));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new PositionedUserData(5, -1, 3));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new PositionedUserData(5, 5, 0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new PositionedUserData(5, 5, -1));
        new PositionedUserData(0, 0, 1);
    }

    @Test
    public void testHighscoresTableData() {
        PositionedUserData a = new PositionedUserData(555, 888, 1);
        PositionedUserData b = new PositionedUserData(778, 555, 2);
        PositionedUserData c = new PositionedUserData(123, 222, 3);
        List<PositionedUserData> p1 = new ArrayList<>(3);
        p1.add(a);
        p1.add(b);
        p1.add(c);
        PositionedUserData d = new PositionedUserData(555, 888, 1);
        PositionedUserData e = new PositionedUserData(778, 555, 2);
        PositionedUserData f = new PositionedUserData(123, 222, 3);
        List<PositionedUserData> p2 = new ArrayList<>(3);
        p2.add(d);
        p2.add(e);
        p2.add(f);
        PositionedUserData g = new PositionedUserData(555, 999, 1);
        PositionedUserData h = new PositionedUserData(558, 555, 2);
        PositionedUserData i = new PositionedUserData(123, 222, 4);
        List<PositionedUserData> p3 = new ArrayList<>(3);
        p3.add(g);
        p3.add(h);
        p3.add(i);
        List<PositionedUserData> p4 = new ArrayList<>(1);
        p4.add(a);
        p4.add(b);
        HighscoresTableData h1 = new HighscoresTableData(p1);
        HighscoresTableData h2 = new HighscoresTableData(p2);
        HighscoresTableData h3 = new HighscoresTableData(p3);
        HighscoresTableData h4 = new HighscoresTableData(p4);
        Assertions.assertTrue(h1.equals(h2));
        Assertions.assertFalse(h1.equals(h3));
        Assertions.assertFalse(h1.equals(h4));
        Assertions.assertFalse(h1.equals(null));
        Assertions.assertFalse(h1.equals("foo"));
        Assertions.assertEquals(h1.hashCode(), h2.hashCode());
        Assertions.assertNotEquals(h1.hashCode(), h3.hashCode());
        Assertions.assertNotEquals(h1.hashCode(), h4.hashCode());
        Assertions.assertEquals("{\"highscores\":[" + a + "," + b + "," + c + "]}", h1.toString());
        Assertions.assertEquals("{\"highscores\":[" + g + "," + h + "," + i + "]}", h3.toString());
    }

    @Test
    public void testHighscoresTableDataBadInstantiation() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new HighscoresTableData(null));
    }
}

package ninja.javahacker.temp.pipatest.tests;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.TreeSet;
import ninja.javahacker.temp.pipatest.avl.ImmutableWeightedAvlTree;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Victor Williams Stafusa da Silva
 */
public class AvlTreeTest {

    /**
     * Test sole constructor.
     */
    public AvlTreeTest() {
    }

    @Test
    public void testSimpleInsertion() {
        ImmutableWeightedAvlTree<Integer, String> avl = new ImmutableWeightedAvlTree<>();
        for (int i = 0; i < 20; i++) {
            avl = avl.put(i, 1, "" + i);
        }
        List<Integer> in1 = new ArrayList<>();
        List<Integer> in2 = new ArrayList<>();
        avl.forEach((x, y, lw, nd, rw) -> {
            in1.add(x);
            in2.add(x);
        });
        in1.sort(Integer::compareTo);
        Assertions.assertEquals(in1, in2);
    }

    @Test
    public void testManyRandomInsertions() {
        ImmutableWeightedAvlTree<String, String> avl = new ImmutableWeightedAvlTree<>();
        int prime1 = 13;
        int prime2 = 101;
        int size = 2000;

        Map<String, String> toCheck = new HashMap<>(size);
        for (int i = 0; i < size; i += 4) {
            int j = i + 1;
            int k = i + 2;
            avl = avl.put("a" + (i * prime1) + "b", 1, "c" + (i * prime2) + "d");
            avl = avl.put("e" + (j * prime1) + "f", 1, "g" + (j * prime2) + "h");
            avl = avl.put("i" + (k * prime1) + "j", 1, "k" + (k * prime2) + "l");
            toCheck.put("a" + (i * prime1) + "b", "c" + (i * prime2) + "d");
            toCheck.put("e" + (j * prime1) + "f", "g" + (j * prime2) + "h");
            toCheck.put("i" + (k * prime1) + "j", "k" + (k * prime2) + "l");
        }
        for (int i = 0; i < size; i += 4) {
            int j = i + 1;
            int k = i + 3;
            avl = avl.remove("e" + (j * prime1) + "f");
            toCheck.remove("e" + (j * prime1) + "f");
            avl = avl.put("m" + (k * prime1) + "n", 1, "o" + (k * prime2) + "p");
            toCheck.put("m" + (k * prime1) + "n", "o" + (k * prime2) + "p");
        }
        for (int i = 2; i < size; i += 4) {
            avl = avl.put("q" + (i * prime1) + "r", 1, "s" + (i * prime2) + "t");
            toCheck.put("q" + (i * prime1) + "r", "s" + (i * prime2) + "t");
        }

        List<String> desiredKeysAccessShouldBe = new ArrayList<>(new TreeSet<>(toCheck.keySet()));
        List<String> keysAccessed = new ArrayList<>(size);
        avl.forEach((x, y, lw, nd, rw) -> {
            keysAccessed.add(x);
            Assertions.assertTrue(toCheck.containsKey(x));
            Assertions.assertEquals(toCheck.get(x), y);
        });
        Assertions.assertEquals(desiredKeysAccessShouldBe, keysAccessed);
    }

    @Test
    public void testSimpleMutation() {
        ImmutableWeightedAvlTree<Integer, String> avl = new ImmutableWeightedAvlTree<>();
        for (int i = 0; i < 20; i++) {
            avl = avl.put(i, 1, "" + i);
        }
        List<Integer> in1 = new ArrayList<>();
        List<Integer> in2 = new ArrayList<>();
        avl.forEach((x, y, lw, nd, rw) -> {
            in1.add(x);
            in2.add(x);
        });
        in1.sort(Integer::compareTo);
        Assertions.assertEquals(in1, in2);
    }

    @Test
    public void testSimpleAddAndRemove() {
        ImmutableWeightedAvlTree<String, String> avl = new ImmutableWeightedAvlTree<>();
        avl = avl.put("a", 1, "a");
        avl = avl.put("e", 1, "e");
        avl = avl.put("i", 1, "i");
        avl = avl.put("b", 1, "b");
        avl = avl.put("f", 1, "f");
        avl = avl.put("j", 1, "j");
        avl = avl.remove("e");
        avl = avl.remove("f");
        Optional<String> out = avl.get("j");
        Assertions.assertTrue(out.isPresent());
        Assertions.assertEquals("j", out.get());
    }

    private static int sumTo(int x) {
        return (x * x + x) / 2;
    }

    @Test
    public void testWeightsOnTraversal() {
        ImmutableWeightedAvlTree<Integer, String> avl = new ImmutableWeightedAvlTree<>();
        int totalNodes = 50;
        for (int i = 0; i < totalNodes; i++) {
            avl = avl.put(i, i, "" + i);
        }
        avl.forEach((x, y, lw, nd, rw) -> {
            Assertions.assertEquals(sumTo(x - 1), lw);
            Assertions.assertEquals(x + 0, nd);
            Assertions.assertEquals(sumTo(totalNodes - 1) - sumTo(x), rw);
        });
        avl.forEachReverse((x, y, lw, nd, rw) -> {
            Assertions.assertEquals(sumTo(x - 1), lw);
            Assertions.assertEquals(x + 0, nd);
            Assertions.assertEquals(sumTo(totalNodes - 1) - sumTo(x), rw);
        });
    }

    @Test
    public void testGetWeights() {
        ImmutableWeightedAvlTree<Integer, String> avl = new ImmutableWeightedAvlTree<>();
        int totalNodes = 50;
        for (int i = 0; i < totalNodes; i++) {
            avl = avl.put(i, i, "" + i);
        }
        Assertions.assertEquals(sumTo(22), avl.getLeftWeight(23).getAsInt());
        Assertions.assertEquals(23, avl.getNodeWeight(23).getAsInt());
        Assertions.assertEquals(sumTo(totalNodes - 1) - sumTo(23), avl.getRightWeight(23).getAsInt());
        Assertions.assertEquals(sumTo(41), avl.getLeftWeight(42).getAsInt());
        Assertions.assertEquals(42, avl.getNodeWeight(42).getAsInt());
        Assertions.assertEquals(sumTo(totalNodes - 1) - sumTo(42), avl.getRightWeight(42).getAsInt());
        Assertions.assertEquals(OptionalInt.empty(), avl.getLeftWeight(9999));
        Assertions.assertEquals(OptionalInt.empty(), avl.getRightWeight(9999));
        Assertions.assertEquals(OptionalInt.empty(), avl.getNodeWeight(9999));
        Assertions.assertEquals(sumTo(totalNodes - 1), avl.getTotalWeight());
    }
}

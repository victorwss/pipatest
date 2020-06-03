package ninja.javahacker.temp.pipatest.avl;

import edu.umd.cs.findbugs.annotations.CheckReturnValue;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.atomic.AtomicReference;
import net.jcip.annotations.Immutable;

/**
 * This is an immutable AVL tree which offer map-like behavior. However, it does not implements the interface {@link Map}
 * because this would unnecessarily complicate its implementation beyond the functionality needed by the highscores table game, prune
 * the opportunity to create a specialized data structures for that, and also, the {@link Map} interface is designed for something mutable,
 * while this implementation is totally immutable.
 *
 * <p>Like all AVL trees, the complexity for the search, insertion and deletion operations are all {@code O(log N)}. Like all
 * {@link Map}s, the keys must be unique, but the values doesn't needs to be. Any attempt to assign two values to the same key, will
 * actually produce a new tree which replaces the first value with the second one with the same key.</p>
 *
 * <p>This implementation is thread-safe and lock-free, using immutable nodes to achieve that. When the tree suffers a mutation in
 * some node, instead of mutating the node, a copy of it is created with the same child nodes (not copies of them). All the ancestor nodes
 * up to the root (and even the tree itself) are also "mutated" in the same way, by creating new nodes. This way, any operations traversing
 * or searching the tree will never notice side-effects of concurrent trees mutations. Effectively, for each mutation, the entire tree is
 * replaced by another tree. Some people might expect that if every mutation actually produces a new tree, it would have very bad
 * performance, but this is not the case because AVL trees have a height proportional to the logarithm of the number of nodes and only the
 * changed nodes and its ancestors needs to be recreated, which effectively means that the cost of mutations is {@code O(log N)}.</p>
 *
 * <p>All the mutating methods returns new AVL trees. A client code maintaining a reference to this AVL tree is responsible to keep it
 * up-to-date and take care of the complexity of that. One suggested method to achieve that in a multi-thread environment, is to use the
 * {@link AtomicReference} class to hold the reference to the tree and use its methods for changing them or to protect it with a
 * {@code synchronized} block.</p>
 *
 * <p>Further, a weight is maintained for each node and the tree computes the weight for all the nodes to the left and to the right of
 * the node, maintaining them into the tree. In use cases where all the nodes should have the same weight, 1 should be assigned as the
 * node weight. In use cases where there is no interest in the weight, it should be assigned to 0.</p>
 *
 * <p>Null key and values aren't accepted.</p>
 *
 * @param <K> The type of the key used to search for nodes.
 * @param <V> The type of the data hold into each node.
 * @author Victor Williams Stafusa da Silva
 */
@Immutable
public class ImmutableWeightedAvlTree<K extends Comparable<K>, V> {

    /**
     * This class represents nodes inside the AVL tree.
     * <p>Note that almost all methods of this class heavily uses recursion.</p>
     * @param <K> The type of the key used to search for nodes.
     * @param <V> The type of the data hold into each node.
     */
    @Immutable
    private static final class Node<K extends Comparable<K>, V> {

        /**
         * The left child of this node, or null if there isn't one.
         */
        @Nullable
        private final Node<K, V> leftChild;

        /**
         * The left right of this node, or null if there isn't one.
         */
        @Nullable
        private final Node<K, V> rightChild;

        /**
         * The node's key.
         */
        @NonNull
        private final K key;

        /**
         * The node's value.
         */
        @NonNull
        private final V value;

        /**
         * The height of the subtree rooted at this node.
         */
        private final int height;

        /**
         * The balance of this node, as defined for AVL trees.
         */
        private final int balance;

        /**
         * The weight of this node.
         */
        private final int nodeWeight;

        /**
         * The weight of the left subtree. Do not confound this with the weight of all the leftmost nodes, since this do not consider
         * nodes that are in the left subtree of the parent node when this node is part of its right subtree, and so on with all the
         * other ancestor nodes up to the root.
         */
        private final int leftWeight;

        /**
         * The weight of the right subtree. Do not confound this with the weight of all the rightmost nodes, since this do not consider
         * nodes that are in the right subtree of the parent node when this node is part of its left subtree, and so on with all the
         * other ancestor nodes up to the root.
         */
        private final int rightWeight;

        /**
         * The total weight of this node and all the nodes in its subtree.
         */
        private final int totalWeight;

        /**
         * Instantiates a node.
         * This constructor should be used only through the
         * {@link Node#createNode(Comparable, Object, int, Node, Node) createNode(K, V, int, Node&lt;K, V&gt;, Node&lt;K, V&gt;)} and
         * {@link Node#leaf(Comparable, int, Object) leaf(K, int, V)} methods, to ensure balance and perform necessary rebalances if needed.
         * @param key The node's key.
         * @param value The node's value.
         * @param nodeWeight The node's weight.
         * @param leftChild The node which is the left child of this one.
         * @param rightChild The node which is the right child of this one.
         */
        private Node(@NonNull K key, @NonNull V value, int nodeWeight, @Nullable Node<K, V> leftChild, @Nullable Node<K, V> rightChild) {

            // Direct field assignments.
            this.nodeWeight = nodeWeight;
            this.leftChild = leftChild;
            this.rightChild = rightChild;
            this.key = key;
            this.value = value;

            // Calculated fields computed from looking up into the child nodes.
            this.leftWeight = leftChild == null ? 0 : leftChild.totalWeight;
            this.rightWeight = rightChild == null ? 0 : rightChild.totalWeight;
            this.totalWeight = nodeWeight + leftWeight + rightWeight;
            int lh = leftChild == null ? 0 : leftChild.height;
            int rh = rightChild == null ? 0 : rightChild.height;
            this.height = Math.max(lh, rh) + 1;
            this.balance = lh - rh;

            // Sanity check.
            if (leftChild != null && leftChild.key.compareTo(key) >= 0) throw new AssertionError();
            if (rightChild != null && rightChild.key.compareTo(key) <= 0) throw new AssertionError();
        }

        /**
         * Creates a node with the given key, value, weight and child nodes. Rebalances are performed in order to assure that the returned
         * node forms a balanced (sub)tree.
         * @param <K> The type of the key used to search for nodes.
         * @param <V> The type of the data hold into each node.
         * @param key The node's key.
         * @param value The node's value.
         * @param nodeWeight The node's weight.
         * @param leftChild The node which is the left child of this one.
         * @param rightChild The node which is the right child of this one.
         * @return The newly created nod.
         */
        @NonNull
        @CheckReturnValue
        public static <K extends Comparable<K>, V> Node<K, V> createNode(
                @NonNull K key,
                @NonNull V value,
                int nodeWeight,
                @Nullable Node<K, V> leftChild,
                @Nullable Node<K, V> rightChild)
        {
            // Create the node and rebalance it.
            Node<K, V> n = new Node<>(key, value, nodeWeight, leftChild, rightChild).rebalance();

            // Sanity check.
            if (n.balance < -1 || n.balance > 1) throw new AssertionError();

            return n;
        }

        /**
         * Rebalance this node. This should be invoked only from the
         * {@link Node#createNode(Comparable, Object, int, Node, Node) createNode(K, V, int, Node&lt;K, V&gt;, Node&lt;K, V&gt;)} method,
         * since there should be no way to get unbalanced nodes other than during the creation of a node.
         * @return The same subtree as given by {@code this} node, but rebalanced. If this node is already balanced, {@code this}
         *     is returned.
         */
        @NonNull
        @CheckReturnValue
        private Node<K, V> rebalance() {
            if (balance >= -1 && balance <= 1) return this;
            Node<K, V> lc = leftChild;
            Node<K, V> rc = rightChild;
            int lh = lc == null ? 0 : lc.height;
            int rh = rc == null ? 0 : rc.height;
            int lb = lc == null ? 0 : lc.balance;
            int rb = rc == null ? 0 : rc.balance;
            int b = lh - rh;
            if (b < -1 && rb > 0) return rightLeftRotate();
            if (b > 1 && lb < 0) return leftRightRotate();
            if (b < -1) return leftLeftRotate();
            if (b > 1) return rightRightRotate();
            return this;
        }

        /**
         * Creates a childless (leaf) node with the given key, value and weight.
         * @param <K> The type of the key used to search for nodes.
         * @param <V> The type of the data hold into each node.
         * @param newKey The node's key.
         * @param nodeWeight The node's weight.
         * @param newValue The node's value.
         * @return The newly created nod.
         */
        @NonNull
        @CheckReturnValue
        public static <K extends Comparable<K>, V> Node<K, V> leaf(@NonNull K newKey, int nodeWeight, @NonNull V newValue) {
            return new Node<>(newKey, newValue, nodeWeight, null, null);
        }

        /**
         * Perform a left-right rotation to rebalance this node. This should only be called from the {@link Node#rebalance() rebalance()}
         * method, which is responsible for checking if this type of rebalance is needed.
         * @return A left-right rotation of the subtree rooted at this node.
         */
        @NonNull
        @CheckReturnValue
        private Node<K, V> leftRightRotate() {
            Node<K, V> a = assertNotNull(leftChild);
            Node<K, V> b = assertNotNull(a.rightChild);
            Node<K, V> c = a.withRightChild(b.leftChild);
            Node<K, V> d = this.withLeftChild(b.rightChild);
            return b.withChildren(c, d);
        }

        /**
         * Perform a right-left rotation to rebalance this node. This should only be called from the {@link Node#rebalance() rebalance()}
         * method, which is responsible for checking if this type of rebalance is needed.
         * @return A left-right rotation of the subtree rooted at this node.
         */
        @NonNull
        @CheckReturnValue
        private Node<K, V> rightLeftRotate() {
            Node<K, V> a = assertNotNull(rightChild);
            Node<K, V> b = assertNotNull(a.leftChild);
            Node<K, V> c = a.withLeftChild(b.rightChild);
            Node<K, V> d = this.withRightChild(b.leftChild);
            return b.withChildren(d, c);
        }

        /**
         * Perform a right-right rotation to rebalance this node. This should only be called from the {@link Node#rebalance() rebalance()}
         * method, which is responsible for checking if this type of rebalance is needed.
         * @return A left-right rotation of the subtree rooted at this node.
         */
        @NonNull
        @CheckReturnValue
        private Node<K, V> rightRightRotate() {
            Node<K, V> a = assertNotNull(leftChild);
            Node<K, V> b = this.withLeftChild(a.rightChild);
            return a.withRightChild(b);
        }

        /**
         * Perform a left-left rotation to rebalance this node. This should only be called from the {@link Node#rebalance() rebalance()}
         * method, which is responsible for checking if this type of rebalance is needed.
         * @return A left-right rotation of the subtree rooted at this node.
         */
        @NonNull
        @CheckReturnValue
        private Node<K, V> leftLeftRotate() {
            Node<K, V> a = assertNotNull(rightChild);
            Node<K, V> b = this.withRightChild(a.leftChild);
            return a.withLeftChild(b);
        }

        /**
         * Creates a new (sub)tree (trying to reuse the most nodes possible from the old one) with a new added node.
         * @param newKey The key of the node to add.
         * @param newWeight The weight of the node to add.
         * @param newValue The value of the node to add.
         * @return A new (sub)tree corresponding from the old one with a new node added.
         */
        @NonNull
        @CheckReturnValue
        public Node<K, V> put(@NonNull K newKey, int newWeight, @NonNull V newValue) {
            Node<K, V> lc = leftChild;
            Node<K, V> rc = rightChild;
            return newKey.compareTo(key) < 0
                    ? withLeftChild(lc == null ? leaf(newKey, newWeight, newValue) : lc.put(newKey, newWeight, newValue))
                    : withRightChild(rc == null ? leaf(newKey, newWeight, newValue) : rc.put(newKey, newWeight, newValue));
        }

        /**
         * Finds the value corresponding to the given key, if it exists.
         * @param findingKey The key to find the corresponding value inside the tree.
         * @return An {@link Optional} containing the found value, if it exists, or an empty one if it does not.
         */
        @NonNull
        @CheckReturnValue
        public Optional<V> get(@NonNull K findingKey) {
            Node<K, V> lc = leftChild;
            Node<K, V> rc = rightChild;
            int cmp = findingKey.compareTo(key);
            return cmp == 0 ? Optional.of(value) : Optional.ofNullable(cmp < 0 ? lc : rc).flatMap(n -> n.get(findingKey));
        }

        /**
         * Gives the total weight of this node and its subtree.
         * @return The total weight of this node and its subtree.
         */
        @NonNull
        @CheckReturnValue
        public int getTotalWeight() {
            return totalWeight;
        }

        /**
         * Gives the total weight of the nodes left to the one with the given key within the subtree rooted at this node.
         * @param findingKey The key to find the node inside the tree.
         * @return An {@link Optional} containing the total weight of the nodes left to the one with the given key within the subtree
         *     rooted at this node, if it exists, or an empty one if it doesn't.
         */
        @NonNull
        @CheckReturnValue
        public OptionalInt getLeftWeight(@NonNull K findingKey) {
            int cmp = findingKey.compareTo(key);
            if (cmp == 0) return OptionalInt.of(leftWeight);
            if (cmp < 0) {
                Node<K, V> lc = leftChild;
                if (lc == null) return OptionalInt.empty();
                return lc.getLeftWeight(findingKey);
            } else {
                Node<K, V> rc = rightChild;
                if (rc == null) return OptionalInt.empty();
                OptionalInt partialAnswer = rc.getLeftWeight(findingKey);
                if (!partialAnswer.isPresent()) return OptionalInt.empty();
                return OptionalInt.of(partialAnswer.getAsInt() + leftWeight + nodeWeight);
            }
        }

        /**
         * Gives the total weight of the nodes right to the one with the given key within the subtree rooted at this node.
         * @param findingKey The key to find the node inside the tree.
         * @return An {@link Optional} containing the total weight of the nodes right to the one with the given key within the subtree
         *     rooted at this node, if it exists, or an empty one if it doesn't.
         */
        @NonNull
        @CheckReturnValue
        public OptionalInt getRightWeight(@NonNull K findingKey) {
            int cmp = findingKey.compareTo(key);
            if (cmp == 0) return OptionalInt.of(rightWeight);
            if (cmp > 0) {
                Node<K, V> rc = rightChild;
                if (rc == null) return OptionalInt.empty();
                return rc.getRightWeight(findingKey);
            } else {
                Node<K, V> lc = leftChild;
                if (lc == null) return OptionalInt.empty();
                OptionalInt partialAnswer = lc.getRightWeight(findingKey);
                if (!partialAnswer.isPresent()) return OptionalInt.empty();
                return OptionalInt.of(partialAnswer.getAsInt() + rightWeight + nodeWeight);
            }
        }

        /**
         * Gives the weight of the node having the given key within the subtree rooted at this node.
         * @param findingKey The key to find the node inside the tree.
         * @return An {@link Optional} containing the weight of the given node, if it exists, or an empty one if it doesn't.
         */
        @NonNull
        @CheckReturnValue
        public OptionalInt getNodeWeight(@NonNull K findingKey) {
            int cmp = findingKey.compareTo(key);
            return cmp == 0
                    ? OptionalInt.of(nodeWeight)
                    : Optional.ofNullable(cmp > 0 ? rightChild : leftChild)
                            .map(c -> c.getNodeWeight(findingKey))
                            .orElseGet(OptionalInt::empty);
        }

        /**
         * Gives a new (sub)tree with the node from the given key removed. If there is no such node, returns this node unchanged.
         * @param removeKey The key of the node to be removed.
         * @return A new (sub)tree with the node from the given key removed. If there is no such node, returns this node unchanged.
         */
        @Nullable
        @CheckReturnValue
        public Node<K, V> remove(@NonNull K removeKey) {
            int comp = removeKey.compareTo(key);
            Node<K, V> lc = leftChild;
            Node<K, V> rc = rightChild;
            if (comp < 0) return lc == null ? this : withLeftChild(lc.remove(removeKey));
            if (comp > 0) return rc == null ? this : withRightChild(rc.remove(removeKey));
            if (lc == null) return rc;
            if (rc == null) return lc;
            return rc.height >= lc.height ? rc.extractMin().withNewRight(lc) : lc.extractMax().withNewLeft(rc);
        }

        /**
         * Gives a new copy of this node and its subtrees with the left subtree replaced by the given node and rebalanced applied if
         * needed. If the given node is already the left children, returns this node unchanged.
         * @param newLeftChild The left subtree replacement.
         * @return A new copy of this node and its subtree with the left subtree replaced by the given node and rebalanced applied if
         *     needed. If the given node is already the left children, returns this node unchanged.
         */
        @NonNull
        @CheckReturnValue
        private Node<K, V> withLeftChild(@Nullable Node<K, V> newLeftChild) {
            return withChildren(newLeftChild, rightChild);
        }

        /**
         * Gives a new copy of this node and its subtrees with the right subtree replaced by the given node and rebalanced applied if
         * needed. If the given node is already the right children, returns this node unchanged.
         * @param newRightChild The right subtree replacement.
         * @return A new copy of this node and its subtree with the right subtree replaced by the given node and rebalanced applied if
         *     needed. If the given node is already the right children, returns this node unchanged.
         */
        @NonNull
        @CheckReturnValue
        private Node<K, V> withRightChild(@Nullable Node<K, V> newRightChild) {
            return withChildren(leftChild, newRightChild);
        }

        /**
         * Gives a new copy of this node with its child nodes replaced by the given node and rebalanced applied if needed. If the given
         * nodes are already the children of this one, then returns this node unchanged.
         * @param newLeftChild The left subtree replacement.
         * @param newRightChild The right subtree replacement.
         * @return A new copy of this node with its child nodes replaced by the given node and rebalanced applied if needed. If the given
         *     nodes are already the children of this one, then returns this node unchanged.
         */
        @NonNull
        @CheckReturnValue
        private Node<K, V> withChildren(@Nullable Node<K, V> newLeftChild, @Nullable Node<K, V> newRightChild) {
            return newLeftChild == leftChild && newRightChild == rightChild
                    ? this
                    : createNode(key, value, nodeWeight, newLeftChild, newRightChild);
        }

        /**
         * Represents a pair of nodes, the one extracted from the tree and a new corresponding and rebalanced subtree without it. The
         * extracted node is meant to eventually become a new root of the subtree. Instances of this class represents intermediate steps
         * in the reconstruction of that subtree.
         *
         * <p>This class should be used as part of the {@link Node#extractMin() extractMin()}, {@link Node#extractMax() extractMax()} and
         * {@link Node#remove(Comparable) remove(K)} methods.</p>
         *
         * @param <K> The type of the key used to search for nodes.
         * @param <V> The type of the data hold into each node.
         * @see Node#extractMin() extractMin()
         * @see Node#extractMax() extractMax()
         * @see Node#remove(Comparable) remove(K)
         */
        @Immutable
        private static class NodeReplace<K extends Comparable<K>, V> {

            /**
             * The extracted node of the subtree that will become its new root node.
             */
            @NonNull
            private final Node<K, V> extracted;

            /**
             * The extracted rebalanced subtree without the extracted node.
             */
            @Nullable
            private final Node<K, V> replacement;

            /**
             * Creates an instance with the given extracted node and the rebalanced subtree without the extracted node.
             * @param extracted The extracted node of the subtree.
             * @param replacement The extracted rebalanced subtree without the extracted node.
             */
            public NodeReplace(@NonNull Node<K, V> extracted, @Nullable Node<K, V> replacement) {
                this.extracted = extracted;
                this.replacement = replacement;
            }

            /**
             * Creates a new subtree with the extracted node as the root, the replacement node as its right subtree, and receives
             * the left subtree as a parameter. As always, rebalancements will be performed if needed.
             * subtree.
             * @param oldLeft The left subtree to be given to this node.
             * @return The newly formed subtree with the extracted root as the root, the replacement subtree as the right child and the
             *     given node as the left child. Rebalancements will be performed if needed.
             */
            @NonNull
            @CheckReturnValue
            public Node<K, V> withNewRight(@Nullable Node<K, V> oldLeft) {
                return extracted.withChildren(oldLeft, replacement);
            }

            /**
             * Creates a new subtree with the extracted node as the root, the replacement node as its left subtree, and receives
             * the right subtree as a parameter. As always, rebalancements will be performed if needed.
             * subtree.
             * @param oldRight The right subtree to be given to this node.
             * @return The newly formed subtree with the extracted root as the root, the replacement subtree as the left child and the
             *     given node as the right child. Rebalancements will be performed if needed.
             */
            @NonNull
            @CheckReturnValue
            public Node<K, V> withNewLeft(@Nullable Node<K, V> oldRight) {
                return extracted.withChildren(replacement, oldRight);
            }

            /**
             * Creates a new instance of this {@code NodeReplace} using this instance's {@link NodeReplace#extracted extracted} value
             * and the given {@code Node} as the {@link NodeReplace#replacement replacement}.
             * @param newReplacement The new instance's {@link NodeReplace#replacement replacement}.
             * @return A new {@code NodeReplace} instance.
             */
            @NonNull
            @CheckReturnValue
            public NodeReplace<K, V> withReplacement(@Nullable Node<K, V> newReplacement) {
                return new NodeReplace<>(extracted, newReplacement);
            }

            /**
             * Gives the extracted rebalanced subtree without the extracted node.
             * @return The extracted rebalanced subtree without the extracted node.
             */
            @NonNull
            @CheckReturnValue
            public Node<K, V> getReplacement() {
                return replacement;
            }
        }

        /**
         * Extract the leftmost node of this tree and gives both the node extracted and a new corresponding and rebalanced subtree without
         * it. This method is used as part of the {@link Node#remove(Comparable) remove(K)} method.
         * @return The node extracted and a new corresponding and rebalanced subtree without it.
         */
        @NonNull
        @CheckReturnValue
        private NodeReplace<K, V> extractMin() {
            if (leftChild == null) return new NodeReplace<>(this, rightChild);
            NodeReplace<K, V> ex = leftChild.extractMin();
            return ex.withReplacement(withLeftChild(ex.getReplacement()));
        }

        /**
         * Extract the leftmost node of this tree and gives both the node extracted and a new corresponding and rebalanced subtree without
         * it. This method is used as part of the {@link Node#remove(Comparable) remove(K)} method.
         * @return The node extracted and a new corresponding and rebalanced subtree without it.
         */
        @NonNull
        @CheckReturnValue
        private NodeReplace<K, V> extractMax() {
            if (rightChild == null) return new NodeReplace<>(this, leftChild);
            NodeReplace<K, V> ex = rightChild.extractMax();
            return ex.withReplacement(withRightChild(ex.getReplacement()));
        }

        /**
         * Throws an {@code AssertionError} if the given value is {@code null} and returns it if it's not.
         * @param <X> The type of the given value.
         * @param value The given value.
         * @return The given value.
         * @throws AssertionError If the given value is {@code null}.
         */
        @NonNull
        private static <X> X assertNotNull(X value) {
            if (value == null) throw new AssertionError();
            return value;
        }

        /**
         * Traverses the nodes of the (sub)tree in order.
         * If the traversal gets an exception, it is stopped and the exception relayed to the caller.
         * @param parentLeftWeight The weight of the nodes left to the parent.
         * @param parentRightWeight The weight of the nodes right to the parent.
         * @param receiver The action to perform with each node and its data.
         */
        public void forEach(int parentLeftWeight, int parentRightWeight, @NonNull TraversalAction<K, V> receiver) {
            if (leftChild != null) leftChild.forEach(parentLeftWeight, parentRightWeight + nodeWeight + rightWeight, receiver);
            receiver.run(key, value, parentLeftWeight + leftWeight, nodeWeight, parentRightWeight + rightWeight);
            if (rightChild != null) rightChild.forEach(parentLeftWeight + nodeWeight + leftWeight, parentRightWeight, receiver);
        }

        /**
         * Traverses the nodes of the (sub)tree in reverse order.
         * If the traversal gets an exception, it is stopped and the exception relayed to the caller.
         * @param parentLeftWeight The weight of the nodes left to the parent.
         * @param parentRightWeight The weight of the nodes right to the parent.
         * @param receiver The action to perform with each node and its data.
         */
        public void forEachReverse(int parentLeftWeight, int parentRightWeight, @NonNull TraversalAction<K, V> receiver) {
            if (rightChild != null) rightChild.forEachReverse(parentLeftWeight + nodeWeight + leftWeight, parentRightWeight, receiver);
            receiver.run(key, value, parentLeftWeight + leftWeight, nodeWeight, parentRightWeight + rightWeight);
            if (leftChild != null) leftChild.forEachReverse(parentLeftWeight, parentRightWeight + nodeWeight + rightWeight, receiver);
        }

        /**
         * Gives a string representation of this node containing its key and value and all the subtrees.
         * @return A string representation of this node containing its key and value and all the subtrees.
         */
        @Override
        @CheckReturnValue
        public String toString() {
            return (leftChild == null ? "-" : "(" + leftChild + ")")
                    + " (" + key + ": " + value + ") "
                    + (rightChild == null ? "-" : "(" + rightChild + ")");
        }
    }

    /**
     * The root of the tree.
     */
    @Nullable
    private final Node<K, V> root;

    /**
     * Creates an initially empty instance of the class.
     */
    public ImmutableWeightedAvlTree() {
        this.root = null;
    }

    /**
     * Internal constructor for creating an instance of this class given a node to be the root of the tree.
     * @param root The node to be the root of the tree.
     */
    private ImmutableWeightedAvlTree(@Nullable Node<K, V> root) {
        this.root = root;
    }

    /**
     * Gives a string representation of this tree containing all its keys and values.
     * @return A string representation of this tree containing all its keys and values.
     */
    @Override
    @CheckReturnValue
    public String toString() {
        return isEmpty() ? "-" : root.toString();
    }

    /**
     * Creates a new tree (trying to reuse the most nodes possible from the old one) with a new added node. If there already is a node
     * with the given key, it will be removed and the new one will be added.
     * @param key The key of the node to add.
     * @param weight The weight of the node to add.
     * @param value The value of the node to add.
     * @return A new tree corresponding from the old one with a new node added.
     */
    @NonNull
    @CheckReturnValue
    public ImmutableWeightedAvlTree<K, V> put(@NonNull K key, int weight, @NonNull V value) {
        return this.remove(key).putNoRemove(key, weight, value);
    }

    /**
     * Internal method that should be used only from within the {@link ImmutableWeightedAvlTree#put(Comparable, int, Object) put(K, int, V)}
     * method. This creates a new tree (trying to reuse the most nodes possible from the old one) with a new added node after ensuring
     * that the node holding the key does not exists in the tree.
     * @param key The key of the node to add.
     * @param weight The weight of the node to add.
     * @param value The value of the node to add.
     * @return A new tree corresponding from the old one with a new node added.
     */
    @NonNull
    @CheckReturnValue
    private ImmutableWeightedAvlTree<K, V> putNoRemove(@NonNull K key, int weight, @NonNull V value) {
        return new ImmutableWeightedAvlTree<>(root == null ? Node.leaf(key, weight, value) : root.put(key, weight, value));
    }

    /**
     * Finds the value corresponding to the given key, if it exists.
     * @param key The key to find the corresponding value inside the tree.
     * @return An {@link Optional} containing the found value, if it exists, or an empty one if it does not.
     */
    @NonNull
    @CheckReturnValue
    public Optional<V> get(@NonNull K key) {
        return root == null ? Optional.empty() : root.get(key);
    }

    /**
     * Gives a new tree with the node from the given key removed. If there is no such node, returns this node unchanged.
     * @param key The key of the node to be removed.
     * @return A new tree with the node from the given key removed. If there is no such node, returns this node unchanged.
     */
    @NonNull
    @CheckReturnValue
    public ImmutableWeightedAvlTree<K, V> remove(@NonNull K key) {
        if (root == null) return this;
        Node<K, V> newRoot = root.remove(key);
        return newRoot == root ? this : new ImmutableWeightedAvlTree<>(newRoot);
    }

    /**
     * Traverses the nodes of the tree in order.
     * If the traversal gets an exception, it is stopped and the exception relayed to the caller.
     * @param receiver The action to perform with each node and its data.
     */
    public void forEach(@NonNull TraversalAction<K, V> receiver) {
        if (root != null) root.forEach(0, 0, receiver);
    }

    /**
     * Traverses the nodes of the tree in reverse order.
     * If the traversal gets an exception, it is stopped and the exception relayed to the caller.
     * @param receiver The action to perform with each node and its data.
     */
    public void forEachReverse(@NonNull TraversalAction<K, V> receiver) {
        if (root != null) root.forEachReverse(0, 0, receiver);
    }

    /**
     * Tells if this tree is empty (i.e. has no nodes).
     * @return {@code true} if this tree is empty or {@code false} otherwise.
     */
    @CheckReturnValue
    public boolean isEmpty() {
        return root == null;
    }

    /**
     * Gives the total weight of the tree.
     * @return The total weight of the tree.
     */
    @CheckReturnValue
    public int getTotalWeight() {
        return root == null ? 0 : root.getTotalWeight();
    }

    /**
     * Gives the total weight of the nodes left to the one with the given key within the tree.
     * @param findingKey The key to find the node inside the tree.
     * @return An {@link Optional} containing the total weight of the nodes left to the one with the given key within the tree, if
     *     it exists, or an empty one if it doesn't.
     */
    @NonNull
    @CheckReturnValue
    public OptionalInt getLeftWeight(@NonNull K findingKey) {
        return root == null ? OptionalInt.empty() : root.getLeftWeight(findingKey);
    }

    /**
     * Gives the total weight of the nodes right to the one with the given key within the tree.
     * @param findingKey The key to find the node inside the tree.
     * @return An {@link Optional} containing the total weight of the nodes right to the one with the given key within the tree, if
     *     it exists, or an empty one if it doesn't.
     */
    @NonNull
    @CheckReturnValue
    public OptionalInt getRightWeight(@NonNull K findingKey) {
        return root == null ? OptionalInt.empty() : root.getRightWeight(findingKey);
    }

    /**
     * Gives the weight of the node having the given key, if it exists.
     * @param findingKey The key to find the node inside the tree.
     * @return An {@link Optional} containing the weight of the given node, if it exists, or an empty one if it doesn't.
     */
    @NonNull
    @CheckReturnValue
    public OptionalInt getNodeWeight(@NonNull K findingKey) {
        return root == null ? OptionalInt.empty() : root.getNodeWeight(findingKey);
    }

    /**
     * Represents an action to be performed with a tree node during a tree traversal.
     * @param <K> The type of the key used to search for nodes.
     * @param <V> The type of the data hold into each node.
     */
    @FunctionalInterface
    public static interface TraversalAction<K extends Comparable<K>, V> {

        /**
         * This is the functional method representing what should be done with each tree node.
         * @param key The key of the node.
         * @param value The value of the node.
         * @param leftWeight The total weight of all the nodes to the left of the current one.
         * @param nodeWeight The total weight of the node.
         * @param rightWeight The total weight of all the nodes to the right of the current one.
         */
        public void run(K key, V value, int leftWeight, int nodeWeight, int rightWeight);
    }
}

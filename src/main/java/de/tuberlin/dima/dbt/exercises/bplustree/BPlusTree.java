package de.tuberlin.dima.dbt.exercises.bplustree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;

/**
 * Implementation of a B+ tree.
 * <p>
 * The capacity of the tree is given by the capacity argument to the
 * constructor. Each node has at least {capacity/2} and at most {capacity} many
 * keys. The values are strings and are stored at the leaves of the tree.
 * <p>
 * For each inner node, the following conditions hold:
 * <p>
 * {pre}
 * Integer[] keys = innerNode.getKeys();
 * Node[] children = innerNode.getChildren();
 * {pre}
 * <p>
 * - All keys in {children[i].getKeys()} are smaller than {keys[i]}.
 * - All keys in {children[j].getKeys()} are greater or equal than {keys[i]}
 * if j > i.
 */
public class BPlusTree {

    ///// Implement these methods


    private LeafNode findLeafNode(Integer key, Node node, Deque<InnerNode> parents) {
        if (node instanceof LeafNode) {
            return (LeafNode) node;
        } else {
            InnerNode innerNode = (InnerNode) node;
            if (parents != null) {
                parents.push(innerNode);
            }
            int index = 0;
            while (index < innerNode.getKeys().length && (innerNode.getKeys()[index] == null || key > innerNode.getKeys()[index])) {
                if (innerNode.getKeys()[index] != null) {
                    index++;
                } else {
                    break; // Break out of the loop when encountering a null value
                }
            }




            return findLeafNode(key, innerNode.getChildren()[index], parents);
        }
    }

    private String lookupInLeafNode(Integer key, LeafNode node) {
        int index = 0;
        while (index < node.getKeys().length && key > node.getKeys()[index]) {
            index++;
        }

        if (index < node.getKeys().length && key.equals(node.getKeys()[index])) {
            return node.getValues()[index];
        } else {
            return null; // Key not found
        }
    }
    private void insertIntoLeafNode(Integer key, String value, LeafNode node, Deque<InnerNode> parents) {

        // Check if the leaf node overflows
        if (countNonNullKeys(node.getKeys()) >= capacity) {
            // Leaf node is full, handle leaf node overflow
            handleLeafNodeOverflow(node, parents, key, value);
        }
        else {
            // Find the index to insert the key in the sorted order
            int index = 0;
            while (index < node.getKeys().length && key > node.getKeys()[index]) {
                index++;
            }

            // Shift elements to make space for the new key-value pair
            for (int i = node.getKeys().length - 1; i > index; i--) {
                node.getKeys()[i] = node.getKeys()[i - 1];
                node.getValues()[i] = node.getValues()[i - 1];
            }

            // Insert the new key-value pair
            node.getKeys()[index] = key;
            node.getValues()[index] = value;
        }

    }

    private void handleLeafNodeOverflow(LeafNode node, Deque<InnerNode> parents, int key, String value) {
        // Split it in half at the median
        int medianIndex = node.getKeys().length / 2;

// Create a temporary array to hold all keys including the new one
        Integer[] tempArray = new Integer[capacity + 1];
        System.arraycopy(node.getKeys(), 0, tempArray, 0, node.getKeys().length);
        tempArray[tempArray.length - 1] = key;
        Arrays.sort(tempArray);

// Create a temporary array to hold all values including the new one
        String[] tempValues = new String[capacity + 1];
        System.arraycopy(node.getValues(), 0, tempValues, 0, node.getValues().length);
        tempValues[tempValues.length - 1] = value;

// Create a new LeafNode for the right half
        LeafNode rightNode = new LeafNode(
                Arrays.copyOfRange(tempArray, medianIndex, tempArray.length),
                Arrays.copyOfRange(tempValues, medianIndex, tempValues.length),
                capacity
        );

        System.out.println(Arrays.toString(rightNode.getKeys()));
        // Update the current leaf node to keep only the left half
        node.setKeys(Arrays.copyOfRange(node.getKeys(), 0, medianIndex));
        node.setValues(Arrays.copyOfRange(node.getValues(), 0, medianIndex));
        System.out.println(Arrays.toString(node.getKeys()));
        // Calculate the median key (middle key moved to the parent)
        Integer medianKey = tempArray[medianIndex];

        // If the parent is null, create a new root
        if (parents.isEmpty()) {
            root = new InnerNode(new Integer[]{medianKey}, new Node[]{node, rightNode}, capacity);
        } else {
            // Insert the median key into the parent and handle parent overflow recursively
            InnerNode parent = parents.pop();
            insertMedianIntoParent(medianKey, node, rightNode, parent, parents);
        }
    }

    private void insertMedianIntoParent(Integer medianKey, Node leftChild, Node rightChild, InnerNode parent, Deque<InnerNode> parents) {
        // Find the index to insert the median key in the sorted order
        int index = 0;
        while (index < parent.getKeys().length && (medianKey == null || medianKey > parent.getKeys()[index] || parent.getKeys()[index] == null)) {
            index++;
        }

        // Shift elements to make space for the new key
        for (int i = parent.getKeys().length - 1; i > index; i--) {
            parent.getKeys()[i] = parent.getKeys()[i - 1];
            parent.getChildren()[i + 1] = parent.getChildren()[i];
        }

        // Insert the median key and update children references
        parent.getKeys()[index] = medianKey;
        parent.getChildren()[index + 1] = rightChild;
        parent.getChildren()[index] = leftChild;

        // Check if the parent overflows and handle accordingly
        if (countNonNullKeys(parent.getKeys()) >= capacity) {
            // Parent is full, handle parent overflow recursively
            InnerNode grandParent = parents.isEmpty() ? null : parents.pop();
            handleInnerNodeOverflow(parent, grandParent, parents);
        }
    }


    private void handleInnerNodeOverflow(InnerNode node, InnerNode parent, Deque<InnerNode> parents) {
        // Split it in half at the median
        int medianIndex = node.getKeys().length / 2;

        // Create a new InnerNode for the right half
        InnerNode rightNode = new InnerNode(Arrays.copyOfRange(node.getKeys(), medianIndex + 1, node.getKeys().length),
                Arrays.copyOfRange(node.getChildren(), medianIndex + 1, node.getChildren().length),
                capacity);

        // Update the current inner node to keep only the left half
        node.setKeys(Arrays.copyOfRange(node.getKeys(), 0, medianIndex));
        node.setChildren(Arrays.copyOfRange(node.getChildren(), 0, medianIndex + 1));

        // Calculate the median key (middle key moved to the parent)
        Integer medianKey = node.getKeys()[medianIndex];

        // If the parent is null, create a new root
        if (parent == null) {
            InnerNode newRoot = new InnerNode(new Integer[]{medianKey}, new Node[]{node, rightNode}, capacity);
            root = newRoot;
        } else {
            // Insert the median key into the parent and handle parent overflow recursively
            insertMedianIntoParent(medianKey, node, rightNode, parent, parents);
        }
    }








    private String deleteFromLeafNode(Integer key, LeafNode node, Deque<InnerNode> parents) {
        // Find the index of the key in the leaf node
        int index = 0;
        while (index < node.getKeys().length && key > node.getKeys()[index]) {
            index++;
        }

        // If key is found, delete and return the corresponding value
        if (index < node.getKeys().length && key.equals(node.getKeys()[index])) {
            String deletedValue = node.getValues()[index];

            // Create a new array excluding the deleted key and values
            Integer[] newKeys = new Integer[node.getKeys().length - 1];
            String[] newValues = new String[node.getValues().length - 1];

            int newIndex = 0;
            for (int i = 0; i < node.getKeys().length; i++) {
                if (i != index) {
                    newKeys[newIndex] = node.getKeys()[i];
                    newValues[newIndex] = node.getValues()[i];
                    newIndex++;
                    System.out.println("----- Keys ------");
                    System.out.println(Arrays.toString(newKeys));
                    System.out.println("----- Values ------");
                    System.out.println(Arrays.toString(newValues));
                    System.out.println("-------------");

                }
            }

            // Set the node's keys and values to the new array
            node.setKeys(newKeys);
            node.setValues(newValues);
            handleLeafNodeUnderflow(node, parents);
            moveKeysToLeft(node);
            return deletedValue;
        } else {
            return null; // Key not found
        }
    }



    private void moveKeysToLeft(LeafNode node) {
        Integer[] tempKeys = new Integer[node.getKeys().length];
        String[] tempValues = new String[node.getValues().length];

        int tempIndex = 0;
        for (int index = 0; index < node.getKeys().length; index++) {
            if (node.getKeys()[index] != null) {
                tempKeys[tempIndex] = node.getKeys()[index];
                tempValues[tempIndex] = node.getValues()[index];
                tempIndex++;
            }
        }

        // Fill the remaining positions with nulls
        while (tempIndex < node.getKeys().length) {
            tempKeys[tempIndex] = null;
            tempValues[tempIndex] = null;
            tempIndex++;
            System.out.println(Arrays.toString(tempKeys));

        }

        // Copy the sorted and null-filled array back to the original array
        System.arraycopy(tempKeys, 0, node.getKeys(), 0, tempKeys.length);
        System.arraycopy(tempValues, 0, node.getValues(), 0, tempValues.length);
    }

    private int countNonNullKeys(Integer[] keys) {
        int count = 0;
        for (Integer key : keys) {
            if (key != null) {
                count++;
            }
        }
        return count;
    }
    private void handleLeafNodeUnderflow(LeafNode node, Deque<InnerNode> parents) {
        // Check if the leaf node is underflowing (has fewer elements than capacity/2)
        if (countNonNullKeys(node.getKeys()) < capacity / 2) {
            // Leaf node is underflowing, need to borrow or merge with siblings
            InnerNode parent = parents.peek();

            // Find the index of the current leaf node in the parent
            int index = 0;
            while (true) {
                if (parent != null)
                    if (!(index < parent.getChildren().length && parent.getChildren()[index] != node)) break;
                index++;
            }

            // Try borrowing from the left sibling
            if (index > 0 && borrowOrMergeWithSibling(index, parent, node, parents, true)) {
                return;
            }

            // Try borrowing from the right sibling
            if (index < parent.getChildren().length - 1 && borrowOrMergeWithSibling(index, parent, node, parents, false)) {
                return;
            }

            // Merge with the left or right sibling if borrowing fails
            if (index > 0) {
                mergeWithSibling(index, parent, node, (LeafNode) parent.getChildren()[index - 1], parents, true);
            } else {
                mergeWithSibling(index, parent, node, (LeafNode) parent.getChildren()[index + 1], parents, false);
            }
        }
    }

    private boolean borrowOrMergeWithSibling(int index, InnerNode parent, LeafNode node, Deque<InnerNode> parents, boolean borrowFromLeft) {
        LeafNode sibling;
        int borrowIndex;
        if (borrowFromLeft && index > 0) {
            sibling = (LeafNode) parent.getChildren()[index - 1];
                borrowIndex = sibling.getKeys().length - 1;
        } else if (!borrowFromLeft && index < parent.getChildren().length - 1) {
            sibling = (LeafNode) parent.getChildren()[index + 1];
            borrowIndex = 0;
        } else {
            return false; // Cannot borrow from left if index is 0 or from right if index is at the last child
        }

        if (sibling.getKeys()[borrowIndex] != null && countNonNullKeys(sibling.getKeys())>capacity/2) {
            // Borrow key-value pair from the sibling
            borrowKeyValueFromSibling(node, sibling, borrowIndex, borrowFromLeft);

            // Update parent key
            parent.getKeys()[index - (borrowFromLeft ? 1 : 0)] = sibling.getKeys()[borrowIndex];

            return true; // Borrowing successful
        } else {

        // Unable to borrow, perform merging
        mergeWithSibling(index, parent, node, sibling, parents, borrowFromLeft);
        return true;
}
        // Borrowing failed, merged instead
    }

    private void borrowKeyValueFromSibling(LeafNode node, LeafNode sibling, int borrowIndex, boolean borrowFromLeft) {
        int nodeIndex = !borrowFromLeft ? node.getKeys().length - 1 : 0;


        // Insert the borrowed key-value pair
        node.getKeys()[nodeIndex] = sibling.getKeys()[borrowIndex];
        node.getValues()[nodeIndex] = sibling.getValues()[borrowIndex];

        // Update the sibling
        sibling.getKeys()[borrowIndex] = null;
        sibling.getValues()[borrowIndex] = null;
        moveKeysToLeft(sibling);
    }

    private void mergeWithSibling(int index, InnerNode parent, LeafNode node, LeafNode sibling, Deque<InnerNode> parents, boolean mergeWithLeft) {
        int siblingIndex = mergeWithLeft ? index - 1 : index + 1;

        // Move all keys and values from the sibling to the node
        int nodeIndex = capacity/2-1;
        System.arraycopy(sibling.getKeys(), 0, node.getKeys(), nodeIndex, sibling.getKeys().length-1);
        System.arraycopy(sibling.getValues(), 0, node.getValues(), nodeIndex, sibling.getValues().length-1);

        // Update the parent key
        parent.getKeys()[siblingIndex - (mergeWithLeft ? 1 : 0)] = null;

        // Update the parent's references
        System.arraycopy(parent.getChildren(), siblingIndex + 1, parent.getChildren(), siblingIndex, parent.getChildren().length - 1 - siblingIndex);

        // Set the last reference in the parent to null
        parent.getChildren()[parent.getChildren().length - 1] = null;

        // Update parents
        updateParents(parents);
    }

    private void updateParents(Deque<InnerNode> parents) {
        while (!parents.isEmpty()) {
            InnerNode parent = parents.pop();

            // Update keys based on the first element of children
            for (int i = 0; i < parent.getKeys().length; i++) {
                if (parent.getChildren()[i] != null) {
                    if (parent.getChildren()[i] instanceof LeafNode) {
                        LeafNode childLeaf = (LeafNode) parent.getChildren()[i];
                        parent.getKeys()[i] = (childLeaf.getKeys()[0] != null) ? childLeaf.getKeys()[0] : null;
                    } else if (parent.getChildren()[i] instanceof InnerNode) {
                        InnerNode childInner = (InnerNode) parent.getChildren()[i];
                        parent.getKeys()[i] = (childInner.getKeys()[0] != null) ? childInner.getKeys()[0] : null;
                    }
                }
            }
            parent.getKeys()[0] = null;
            for (int i = 0; i < parent.getKeys().length - 1; i++) {
                parent.getKeys()[i] = parent.getKeys()[i + 1];
            }
            // Move up to the next level of parents
        }
    }




    ///// Public API
    ///// These can be left unchanged

    /**
     * Lookup the value stored under the given key.
     *
     * @return The stored value, or {null} if the key does not exist.
     */
    public String lookup(Integer key) {
        LeafNode leafNode = findLeafNode(key, root);
        return lookupInLeafNode(key, leafNode);
    }

    /**
     * Insert the key/value pair into the B+ tree.
     */
    public void insert(int key, String value) {
        Deque<InnerNode> parents = new LinkedList<>();
        LeafNode leafNode = findLeafNode(key, root, parents);
        insertIntoLeafNode(key, value, leafNode, parents);
    }

    /**
     * Delete the key/value pair from the B+ tree.
     *
     * @return The original value, or {null} if the key does not exist.
     */
    public String delete(Integer key) {
        Deque<InnerNode> parents = new LinkedList<>();
        LeafNode leafNode = findLeafNode(key, root, parents);
        return deleteFromLeafNode(key, leafNode, parents);
    }

    ///// Leave these methods unchanged

    private int capacity = 0;

    private Node root;

    public BPlusTree(int capacity) {
        this(new LeafNode(capacity), capacity);
    }

    public BPlusTree(Node root, int capacity) {
        assert capacity % 2 == 0;
        this.capacity = capacity;
        this.root = root;
    }

    public Node rootNode() {
        return root;
    }

    public String toString() {
        return new BPlusTreePrinter(this).toString();
    }

    private LeafNode findLeafNode(Integer key, Node node) {
        return findLeafNode(key, node, null);
    }

}

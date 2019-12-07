package org.tdf.trie;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Node {
    private static final int BRANCH_SIZE = 17;

    enum Type {
        BRANCH,
        EXTENSION,
        LEAF
    }

    // if node is branch node, the length of children is 17
    // the first 16 element is children, and the 17th element is value
    // if node is extension node or leaf node, the length of children is 2
    // the first element is key and the second element is TrieKey
    private Object[] children;

    static Node newBranch() {
        return Node.builder().children(new Object[BRANCH_SIZE]).build();
    }

    static Node newLeaf(TrieKey key, byte[] value) {
        return builder().children(new Object[]{key, value}).build();
    }

    static Node newExtension(TrieKey key, Node child) {
        return builder().children(new Object[]{key, child}).build();
    }

    private Node newShort(TrieKey key, Object o) {
        // if size of key is zero, we not need to wrap child
        if (key.size() == 0 && o instanceof Node) return (Node) o;
        return builder().children(new Object[]{key, o}).build();
    }

    public Type getType() {
        if (children.length == BRANCH_SIZE) return Type.BRANCH;
        return children[1] instanceof Node ? Type.EXTENSION : Type.LEAF;
    }

    public void setValue(byte[] value) {
        assertBranchOrLeaf();
        if (getType() == Type.BRANCH) {
            children[BRANCH_SIZE - 1] = value;
            return;
        }
        children[1] = value;
    }

    public byte[] getValue() {
        assertBranchOrLeaf();
        if (getType() == Type.BRANCH) return (byte[]) children[BRANCH_SIZE - 1];
        return (byte[]) children[1];
    }

    public byte[] get(TrieKey key) {
        Type type = getType();
        if (type == Type.BRANCH) {
            if (key.isEmpty()) return getValue();
            Node child = getChild(key.get(0));
            return child == null ? null : child.get(key.shift());
        }
        TrieKey k1 = key.matchAndShift(getKey());
        if (k1 == null) return null;
        if (type == Type.LEAF) return k1.isEmpty() ? getValue() : null;
        return getExtension().get(k1);
    }

    void insert(TrieKey key, @NonNull byte[] value) {
        Type type = getType();
        if (type == Type.BRANCH) {
            branchInsert(key, value);
            return;
        }
        TrieKey current = getKey();
        TrieKey commonPrefix = key.getCommonPrefix(current);

        // space is not enough, convert to branch node
        if (commonPrefix.isEmpty() || (commonPrefix.equals(current)) && type != Type.LEAF) {
            toBranch();
            branchInsert(key, value);
            return;
        }

        Object o = children[1];
        Node newBranch = newBranch();
        children[1] = newBranch;
        // reset to common prefix
        children[0] = commonPrefix;

        TrieKey tmp = current.matchAndShift(commonPrefix);
        // tmp is empty -> commonPrefix.equals(current) -> type is leaf
        if (tmp.isEmpty()) {
            newBranch.children[BRANCH_SIZE - 1] = o;
        } else {
            // tmp is not empty -> !commonPrefix.equals(current)
            newBranch.children[tmp.get(0)] = newShort(tmp.shift(), o);
        }

        tmp = key.matchAndShift(commonPrefix);
        if (tmp.isEmpty()) {
            newBranch.children[BRANCH_SIZE - 1] = value;
        } else {
            newBranch.children[tmp.get(0)] = newLeaf(tmp.shift(), value);
        }
    }

    private void insert(TrieKey key, Node child) {
        if (key.isEmpty() && child.getType() != Type.LEAF) {
            throw new RuntimeException("empty key of extension node");
        }

    }

    private void branchInsert(TrieKey key, byte[] value) {
        if (key.isEmpty()) {
            setValue(value);
            return;
        }
        Node child = getChild(key.get(0));
        if (child != null) {
            child.insert(key.shift(), value);
            return;
        }
        child = newLeaf(key.shift(), value);
        children[key.get(0)] = child;
    }

    public Node getChild(int index) {
        assertBranch();
        return (Node) children[index];
    }

    public Node getExtension() {
        assertExtension();
        return (Node) children[1];
    }

    public TrieKey getKey() {
        assertNotBranch();
        return (TrieKey) children[0];
    }

    private void setKey(TrieKey key) {
        assertNotBranch();
        children[0] = key;
    }

    private void assertBranchOrLeaf() {
        if (getType() != Type.BRANCH && getType() != Type.LEAF) throw new RuntimeException("not a branch or leaf node");
    }

    private void assertNotBranch() {
        if (getType() == Type.BRANCH) throw new RuntimeException("not a extension or leaf node");
    }

    private void assertBranch() {
        if (getType() != Type.BRANCH) throw new RuntimeException("not a branch node");
    }

    private void assertExtension() {
        if (getType() != Type.EXTENSION) throw new RuntimeException("not an extension node");
    }

    private void assertLeaf() {
        if (getType() != Type.LEAF) throw new RuntimeException("not a leaf node");
    }

    // convert extension or leaf node to branch
    private void toBranch() {
        TrieKey key = getKey();
        Object o = children[1];
        children = new Object[BRANCH_SIZE];
        children[key.get(0)] = newShort(key.shift(), o);
    }

}

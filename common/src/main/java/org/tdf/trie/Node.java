package org.tdf.trie;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;

/**
 * patricia tree's node inspired by:
 * https://ethfans.org/toya/articles/588
 * https://medium.com/shyft-network-media/understanding-trie-databases-in-ethereum-9f03d2c3325d
 * https://github.com/ethereum/wiki/wiki/Patricia-Tree#optimization
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
class Node {
    private static final int BRANCH_SIZE = 17;

    private boolean dirty;

    private byte[] hash;

    enum Type {
        BRANCH,
        EXTENSION,
        LEAF
    }

    // if node is branch node, the length of children is 17
    // the first 16 element is children, and the 17th element is value
    // if node is extension node or leaf node, the length of children is 2
    // the first element is trie key and the second element is value(leaf node) or child node(extension node)
    private Object[] children;

    static Node newBranch() {
        return Node.builder()
                .children(new Object[BRANCH_SIZE])
                .dirty(true).build();
    }

    static Node newLeaf(TrieKey key, byte[] value) {
        return builder()
                .children(new Object[]{key, value})
                .dirty(true).build();
    }

    static Node newExtension(TrieKey key, Node child) {
        return builder()
                .children(new Object[]{key, child})
                .dirty(true)
                .build();
    }

    // wrap o to an extension or leaf node
    private Node newShort(TrieKey key, Object o) {
        // if size of key is zero, we not need to wrap child
        if (key.size() == 0 && o instanceof Node) {
            return (Node) o;
        }
        return builder()
                .children(new Object[]{key, o})
                .dirty(true)
                .build();
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

    // deep-first scanning
    void traverse(ScannerAction action) {
        Type type = getType();
        action.accept(this);
        if (type == Type.BRANCH) {
            for (int i = 0; i < BRANCH_SIZE - 1; i++) {
                if (children[i] == null) continue;
                action.accept((Node) children[i]);
            }
            return;
        }
        if (type == Type.EXTENSION) {
            action.accept((Node) children[1]);
        }
    }

    void insert(TrieKey key, @NonNull byte[] value) {
        Type type = getType();
        if (type == Type.BRANCH) {
            branchInsert(key, value);
            return;
        }

        TrieKey current = getKey();
        // by definition, common prefix <= current and common prefix <= key ( <= represents subset of here )
        TrieKey commonPrefix = key.getCommonPrefix(current);

        // current is leaf and current equals to key
        if(type == Type.LEAF && commonPrefix.size() == current.size() && commonPrefix.size() == key.size()){
            setValue(value);
            return;
        }

        // space is not enough, convert to branch node
        if (commonPrefix.isEmpty() || (type == Type.LEAF && commonPrefix.size() == current.size())) {
            toBranch();
            branchInsert(key, value);
            return;
        }

        // current is extension and common prefix equals to current
        if (type == Type.EXTENSION && commonPrefix.size() == current.size()) {
            // TODO: remove this assertion for the extension must be branch
            getExtension().assertBranch();
            getExtension().branchInsert(key.shift(commonPrefix.size()), value);
            return;
        }

        // common prefix is a strict subset of current here
        // common prefix < current => tmp couldn't be empty
        TrieKey tmp = current.shift(commonPrefix.size());

        Object o = children[1];
        Node newBranch = newBranch();
        children[1] = newBranch;
        // reset to common prefix
        children[0] = commonPrefix;

        newBranch.children[tmp.get(0)] = newShort(tmp.shift(), o);

        tmp = key.shift(commonPrefix.size());
        if (tmp.isEmpty()) {
            // tmp is empty => common prefix = key => key < current
            newBranch.children[BRANCH_SIZE - 1] = value;
        } else {
            newBranch.children[tmp.get(0)] = newLeaf(tmp.shift(), value);
        }
    }

    Node delete(TrieKey key) {
        Type type = getType();
        if (type == Type.BRANCH) {
            return branchDelete(key);
        }
        TrieKey k1 = key.matchAndShift(getKey());
        if (k1 == null) return this;
        if (type == Type.LEAF) {
            if (k1.isEmpty()) {
                children[1] = null;
                // delete value success, set this to null
                return null;
            }
            // delete failed, no need to compact
            return this;
        }
        Node child = (Node) children[1];
        children[1] = child.delete(k1);
        if (children[1] == null) return null;
        tryCompact();
        return this;
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

    private Node branchDelete(TrieKey key) {
        if (key.isEmpty()) {
            children[BRANCH_SIZE - 1] = null;
            tryCompact();
            return this;
        }
        int idx = key.get(0);
        Node child = (Node) children[idx];
        // delete failed, no need to compact
        if (child == null) return this;
        children[idx] = child.delete(key.shift());
        tryCompact();
        return this;
    }

    private void tryCompact() {
        Type type = getType();
        if (type == Type.LEAF) return;
        if (type == Type.EXTENSION) {
            extensionCompact();
            return;
        }
        int index = getCompactIndex();
        if (index < 0) return;
        branchCompact(index);
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
    // if node is extension, the key couldn't be empty
    // if node is leaf and key is empty, just move value to new branch
    private void toBranch() {
        TrieKey key = getKey();
        Object o = children[1];
        children = new Object[BRANCH_SIZE];
        if (key.isEmpty() && o instanceof byte[]) {
            children[BRANCH_SIZE - 1] = o;
            return;
        }
        children[key.get(0)] = newShort(key.shift(), o);
    }

    // check the branch node could be compacted
    private int getCompactIndex() {
        int cnt = 0;
        int idx = -1;
        for (int i = 0; i < BRANCH_SIZE; i++) {
            if (children[i] != null) {
                cnt++;
                if (cnt > 1) return -1;
                idx = i;
            }
        }
        return idx;
    }

    // join extension node and its non-branch child as a compact extension node
    private void extensionCompact() {
        Node n = getExtension();
        if (n.getType() == Type.BRANCH) return;
        children[0] = getKey().concat(n.getKey());
        children[1] = n.children[1];
    }

    // compact single child or single value branch node to a extension or leaf node
    private void branchCompact(int index) {
        Object o = children[index];
        children = new Object[2];
        if (o instanceof byte[]) {
            children[0] = TrieKey.empty(true);
            children[1] = o;
            return;
        }
        Node n = (Node) o;
        if (n.getType() != Type.BRANCH) {
            // non-branch child could be compressed
            children[0] = TrieKey.single(index).concat((TrieKey) n.children[0]);
            children[1] = n.children[1];
            return;
        }
        children[0] = TrieKey.single(index);
        children[1] = n;
    }
}

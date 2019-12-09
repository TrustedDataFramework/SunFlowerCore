package org.tdf.trie;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import org.tdf.common.Store;
import org.tdf.serialize.RLPElement;
import org.tdf.serialize.RLPItem;
import org.tdf.serialize.RLPList;

import static org.tdf.trie.TrieKey.EMPTY;

/**
 * patricia tree's node inspired by:
 * https://ethfans.org/toya/articles/588
 * https://medium.com/shyft-network-media/understanding-trie-databases-in-ethereum-9f03d2c3325d
 * https://github.com/ethereum/wiki/wiki/Patricia-Tree#optimization
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
class Node {
    private static final int BRANCH_SIZE = 17;
    private static final int MAX_KEY_SIZE = 32;
    private boolean dirty;

    private void setDirty(){
        dirty = true;
        rlp = null;
    }

    // if rlp is a list, it is actual encoding of this node
    // if rlp is a item, it is a hash, its actual rlp encoding is stored in cache
    private RLPElement rlp;

    private Store<byte[], byte[]> cache;

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

    static Node fromEncoded(byte[] encoded, Store<byte[], byte[]> cache) {
        return fromEncoded(RLPElement.fromEncoded(encoded), cache);
    }

    static Node fromEncoded(RLPElement rlp, Store<byte[], byte[]> cache) {
        return Node.builder()
                .rlp(rlp)
                .cache(cache)
                .build();
    }

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

    // encode and commit root node to store
    // return rlp encoded
    public RLPElement encodeAndCommit(HashFunction function, Store<byte[], byte[]> cache) {
        if (!dirty) return rlp;
        RLPList encoded;
        Type type = getType();
        switch (type) {
            case LEAF: {
                encoded = RLPList.createEmpty(2);
                encoded.add(RLPItem.fromBytes(getKey().toPacked(true)));
                encoded.add(RLPItem.fromBytes(getValue()));
                break;
            }
            case EXTENSION: {
                encoded = RLPList.createEmpty(2);
                encoded.add(RLPItem.fromBytes(getKey().toPacked(false)));
                encoded.add(getExtension().encodeAndCommit(function, cache));
                break;
            }
            default: {
                encoded = RLPList.createEmpty(BRANCH_SIZE);
                for (int i = 0; i < BRANCH_SIZE - 1; i++) {
                    Node child = (Node) children[i];
                    if (child == null) {
                        encoded.add(RLPItem.NULL);
                        continue;
                    }
                    encoded.add(child.encodeAndCommit(function, cache));
                }
                encoded.add(RLPItem.fromBytes(getValue()));
            }
        }
        dirty = false;
        byte[] raw = encoded.getEncoded();
        if (raw.length > MAX_KEY_SIZE) {
            byte[] hash = function.apply(raw);
            cache.put(hash, raw);
            return RLPItem.fromBytes(hash);
        }
        return encoded;
    }

    // get actual rlp encoding in the cache
    private void resolve(){
        if(rlp.isList()) return;
        byte[] hash = rlp.getAsItem().get();
        rlp = cache.get(hash).map(RLPElement::fromEncoded)
                .orElseThrow(() -> new RuntimeException("rlp encoding not found in cache"));
    }

    // parse encoded from cache
    private void parse() {
        // has parsed
        if (children != null) return;
        resolve();
        RLPList rlp = this.rlp.getAsList();
        if (rlp.size() == 2) {
            children = new Object[2];
            byte[] packed = rlp.get(0).getAsItem().get();
            TrieKey key = TrieKey.fromPacked(packed);
            children[0] = key;
            boolean terminal = TrieKey.isTerminal(packed);
            if (terminal) {
                children[1] = rlp.get(1).getAsItem().get();
                return;
            }
            children[1] = fromEncoded(rlp.get(1), cache);
            return;
        }
        children = new Object[BRANCH_SIZE];
        for (int i = 0; i < BRANCH_SIZE - 1; i++) {
            if (rlp.get(i).isNull()) continue;
            children[i] = fromEncoded(rlp.get(i), cache);
        }
        RLPItem item = rlp.get(BRANCH_SIZE - 1).getAsItem();
        if (item.isNull()) return;
        children[BRANCH_SIZE - 1] = item.get();
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
        parse();
        if (children.length == BRANCH_SIZE) return Type.BRANCH;
        return children[1] instanceof Node ? Type.EXTENSION : Type.LEAF;
    }

    private void setValue(byte[] value) {
        assertBranchOrLeaf();
        if (getType() == Type.BRANCH) {
            children[BRANCH_SIZE - 1] = value;
            return;
        }
        children[1] = value;
    }

    public byte[] getValue() {
        assertBranchOrLeaf();
        parse();
        if (getType() == Type.BRANCH) return (byte[]) children[BRANCH_SIZE - 1];
        return (byte[]) children[1];
    }

    public byte[] get(TrieKey key) {
        parse();
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
    void traverse(TrieKey init, ScannerAction action) {
        Type type = getType();
        if (type == Type.BRANCH) {
            action.accept(init, this);
            for (int i = 0; i < BRANCH_SIZE - 1; i++) {
                if (children[i] == null) continue;
                ((Node) children[i]).traverse(init.concat(TrieKey.single(i)), action);
            }
            return;
        }
        if (type == Type.EXTENSION) {
            TrieKey path = init.concat(getKey());
            action.accept(path, this);
            getExtension().traverse(path, action);
            return;
        }
        action.accept(init.concat(getKey()), this);
    }

    void insert(TrieKey key, @NonNull byte[] value) {
        parse();
        Type type = getType();
        if (type == Type.BRANCH) {
            branchInsert(key, value);
            return;
        }

        TrieKey current = getKey();
        // by definition, common prefix <= current and common prefix <= key ( <= represents subset of here )
        TrieKey commonPrefix = key.getCommonPrefix(current);

        // current is leaf and current equals to key
        if (type == Type.LEAF && commonPrefix.size() == current.size() && commonPrefix.size() == key.size()) {
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
            return;
        }
        newBranch.children[tmp.get(0)] = newLeaf(tmp.shift(), value);
    }

    Node delete(TrieKey key) {
        parse();
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

    private Node getChild(int index) {
        assertBranch();
        return (Node) children[index];
    }

    private Node getExtension() {
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
        // last non-null children
        int idx = -1;
        for (int i = 0; i < BRANCH_SIZE; i++) {
            if (children[i] == null) continue;
            cnt++;
            if (cnt > 1) return -1;
            idx = i;
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
            children[0] = EMPTY;
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


    private byte[] getValueFromCache(byte[] reference) {
        if (reference.length < MAX_KEY_SIZE) return reference;
        return cache.get(reference)
                .orElseThrow(() -> new RuntimeException("parse node failed: key not found"));
    }
}

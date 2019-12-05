package org.tdf.trie;

import lombok.NonNull;
import org.tdf.common.Store;
import org.tdf.store.MapStore;

// enhanced radix tree
public class TrieImpl {
    private Store<byte[], byte[]> cache;

    private Node root;

    public TrieImpl() {
        this.cache = new MapStore<>();
    }

    public TrieImpl(Store<byte[], byte[]> store) {
        this.cache = store;
    }


    private byte[] get(Node n, TrieKey k) {
        if (n.isNull()) return null;
        Node.Type type = n.getType();
        if (type == Node.Type.BRANCH) {
            BranchNode bn = n.getAsBranch();
            if (k.isEmpty()) return bn.getValue();
            Node childNode = bn.getChild(k.get(0));
            return get(childNode, k.shift());
        }

        TrieKey k1 = k.matchAndShift(n.getKey());
        if (k1 == null) return null;
        if (type == Node.Type.LEAF) {
            return k1.isEmpty() ? n.getAsLeaf().getValue() : null;
        }
        return get(n.getAsExtension().getChild(), k1);
    }

    public void put(byte[] key, @NonNull byte[] value) {
    }


    private Node insert(Node n, TrieKey k, @NonNull byte[] value) {
        if(n.isNull()) return new LeafNode(k, value);

        Node.Type type = n.getType();
        if (type == Node.Type.BRANCH) {
            return insertIntoBranchNode(n.getAsBranch(), k, value);
        }
        TrieKey current = n.getKey();
        TrieKey commonPrefix = k.getCommonPrefix(current);
        if(commonPrefix.isEmpty()){
            BranchNode branchNode = new BranchNode();
        }
        return null;
    }

    private Node insertIntoBranchNode(BranchNode node, TrieKey key, Node child){
        return null;
    }

    private Node insertIntoBranchNode(BranchNode node, TrieKey key, byte[] value) {
        if (key.isEmpty()) {
            node.setValue(value);
            return node;
        }
        Node childNode = node.getChild(key.get(0));
        if (!childNode.isNull()) {
            node.setChild(key.get(0), insert(childNode, key.shift(), value));
            return node;
        }
        TrieKey childKey = key.shift();
        node.setChild(key.get(0), new LeafNode(childKey, value));
        return node;
    }

    private Node insertIntoExtensionNode(ExtensionNode node, TrieKey key, byte[] value) {
        throw new RuntimeException("unimplemented");
    }
}

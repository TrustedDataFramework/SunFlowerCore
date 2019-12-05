package org.tdf.trie;

import lombok.Getter;

@Getter
public class LeafNode implements Node{
    private TrieKey key;
    private boolean dirty;

    private byte[] value;

    public LeafNode(TrieKey key, byte[] value) {
        this.key = key;
        this.value = value;
        dirty = true;
    }

    @Override
    public Type getType() {
        return Type.LEAF;
    }

    @Override
    public boolean isNull() {
        return false;
    }

    public void setValue(byte[] value) {
        this.value = value;
        this.dirty = true;
    }

    @Override
    public BranchNode getAsBranch() {
        throw new RuntimeException("leaf node is not branch");
    }

    @Override
    public ExtensionNode getAsExtension() {
        throw new RuntimeException("leaf node is not extension");
    }

    @Override
    public LeafNode getAsLeaf() {
        return this;
    }
}

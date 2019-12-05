package org.tdf.trie;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
public class ExtensionNode implements Node{
    private TrieKey key;
    private boolean dirty;

    public ExtensionNode(TrieKey key, Node child) {
        this.key = key;
        this.child = child;
        this.dirty = true;
    }

    @Getter
    private Node child;

    @Override
    public Type getType() {
        return Type.EXTENSION;
    }

    @Override
    public boolean isNull() {
        return false;
    }

    public void setChild(Node child) {
        this.child = child;
        this.dirty = true;
    }

    @Override
    public BranchNode getAsBranch() {
        throw new RuntimeException("extension node is not branch");
    }

    @Override
    public ExtensionNode getAsExtension() {
        return this;
    }

    @Override
    public LeafNode getAsLeaf() {
        throw new RuntimeException("extension node is not leaf");
    }
}

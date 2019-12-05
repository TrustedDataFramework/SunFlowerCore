package org.tdf.trie;

public class NullNode implements Node{
    @Override
    public Type getType() {
        return Type.NULL;
    }

    @Override
    public boolean isNull() {
        return true;
    }

    @Override
    public boolean isDirty() {
        return false;
    }

    @Override
    public BranchNode getAsBranch() {
        throw new RuntimeException("not a branch node");
    }

    @Override
    public ExtensionNode getAsExtension() {
        throw new RuntimeException("not a extension node");
    }

    @Override
    public LeafNode getAsLeaf() {
        throw new RuntimeException("not a leaf node");
    }
}

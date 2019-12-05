package org.tdf.trie;

public interface Node {
    Node NULL = new NullNode();

    enum Type {
        NULL,
        BRANCH,
        EXTENSION,
        LEAF
    }

    Type getType();

    boolean isNull();

    boolean isDirty();

    BranchNode getAsBranch();

    ExtensionNode getAsExtension();

    LeafNode getAsLeaf();

    default TrieKey getKey() {
        return getType() == Type.EXTENSION ? getAsExtension().getKey() : getAsLeaf().getKey();
    }
}

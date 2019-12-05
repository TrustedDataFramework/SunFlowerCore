package org.tdf.trie;

import lombok.Getter;

public class BranchNode implements Node {
    private Node[] children = new Node[]{
            NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
            NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL
    };

    public BranchNode() {
        dirty = true;
    }

    @Getter
    private boolean dirty;

    @Getter
    private byte[] value;

    @Override
    public Type getType() {
        return Type.BRANCH;
    }

    @Override
    public boolean isNull() {
        return false;
    }

    public Node getChild(int index) {
        return children[index];
    }

    public void setChild(int index, Node node) {
        children[index] = node;
        dirty = true;
    }

    public void setValue(byte[] value){
        this.value = value;
        dirty = true;
    }

    public void deleteValue(){
        this.value = null;
        dirty = true;
    }

    @Override
    public BranchNode getAsBranch() {
        return this;
    }

    @Override
    public ExtensionNode getAsExtension() {
        throw new RuntimeException("branch node is not extension");
    }

    @Override
    public LeafNode getAsLeaf() {
        throw new RuntimeException("branch node is not leaf");
    }
}

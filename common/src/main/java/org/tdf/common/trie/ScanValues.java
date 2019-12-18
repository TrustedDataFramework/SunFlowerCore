package org.tdf.common.trie;

import lombok.AccessLevel;
import lombok.Getter;
import org.tdf.common.util.ByteArraySet;

@Getter(AccessLevel.PACKAGE)
class ScanValues implements ScannerAction {
    private ByteArraySet bytes = new ByteArraySet();

    @Override
    public void accept(TrieKey path, Node node) {
        if (node.getType() != Node.Type.EXTENSION && node.getValue() != null) {
            bytes.add(node.getValue());
        }
    }
}

package org.tdf.common.trie;

import lombok.AccessLevel;
import lombok.Getter;
import org.tdf.common.util.ByteArraySet;

import java.util.Set;

@Getter(AccessLevel.PACKAGE)
class DumpKeys implements ScannerAction {
    private Set<byte[]> keys = new ByteArraySet();

    @Override
    public Boolean apply(TrieKey path, Node node) {
        if (node.getHash() != null) {
            keys.add(node.getHash());
        }
        return true;
    }
}

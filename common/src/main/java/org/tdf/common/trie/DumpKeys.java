package org.tdf.common.trie;

import lombok.AccessLevel;
import lombok.Getter;
import org.tdf.common.util.ByteArraySet;
import org.tdf.common.util.HexBytes;

import java.util.HashSet;
import java.util.Set;

@Getter(AccessLevel.PACKAGE)
class DumpKeys implements ScannerAction {
    private Set<HexBytes> keys = new HashSet<>();

    @Override
    public Boolean apply(TrieKey path, Node node) {
        if (node.getHash() != null) {
            keys.add(HexBytes.fromBytes(node.getHash()));
        }
        return true;
    }
}

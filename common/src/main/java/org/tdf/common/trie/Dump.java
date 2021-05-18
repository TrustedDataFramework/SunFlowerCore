package org.tdf.common.trie;

import lombok.AccessLevel;
import lombok.Getter;
import org.tdf.common.util.HexBytes;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Getter(AccessLevel.PACKAGE)
class Dump implements ScannerAction {
    private Map<HexBytes, HexBytes> pairs = new HashMap<>();

    @Override
    public Boolean apply(TrieKey path, Node node) {
        if (node.getHash() != null) {
            pairs.put(
                HexBytes.fromBytes(node.getHash()),
                HexBytes.fromBytes(Objects.requireNonNull(node.rlp.getEncoded()))
            );
        }
        return true;
    }
}

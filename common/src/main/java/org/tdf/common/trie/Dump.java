package org.tdf.common.trie;

import lombok.AccessLevel;
import lombok.Getter;
import org.tdf.common.util.ByteArrayMap;

import java.util.Map;
import java.util.Objects;

@Getter(AccessLevel.PACKAGE)
class Dump implements ScannerAction {
    private Map<byte[], byte[]> pairs = new ByteArrayMap<>();

    @Override
    public Boolean apply(TrieKey path, Node node) {
        if (node.getHash() != null) {
            pairs.put(node.getHash(), Objects.requireNonNull(node.rlp).getEncoded());
        }
        return true;
    }
}

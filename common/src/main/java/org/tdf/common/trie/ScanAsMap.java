package org.tdf.common.trie;

import lombok.AccessLevel;
import lombok.Getter;
import org.tdf.common.util.ByteArrayMap;

import java.util.Map;

@Getter(AccessLevel.PACKAGE)
class ScanAsMap implements ScannerAction {
    private Map<byte[], byte[]> map = new ByteArrayMap<>();

    @Override
    public Boolean apply(TrieKey trieKey, Node node) {
        if (node.getType() != Node.Type.EXTENSION && node.getValue() != null) {
            map.put(trieKey.toNormal(), node.getValue());
        }
        return true;
    }
}

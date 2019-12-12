package org.tdf.trie;

import java.util.function.BiConsumer;

public interface ScannerAction extends BiConsumer<TrieKey, Node> {
}

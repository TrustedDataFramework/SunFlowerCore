package org.tdf.common.trie;

import java.util.function.BiFunction;

public interface ScannerAction extends BiFunction<TrieKey, Node, Boolean> {
}

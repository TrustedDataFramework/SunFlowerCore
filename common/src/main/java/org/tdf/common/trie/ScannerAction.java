package org.tdf.common.trie;

import java.util.function.BiFunction;

interface ScannerAction extends BiFunction<TrieKey, Node, Boolean> {
}

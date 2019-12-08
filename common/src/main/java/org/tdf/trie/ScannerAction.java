package org.tdf.trie;

public interface ScannerAction {
    void accept(TrieKey path, Node node);
}

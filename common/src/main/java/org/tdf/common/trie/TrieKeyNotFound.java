package org.tdf.common.trie;

public class TrieKeyNotFound extends RuntimeException{
    public TrieKeyNotFound(){
        super("key not found in the trie");
    }
}

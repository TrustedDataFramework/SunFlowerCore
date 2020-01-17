package org.tdf.common.trie;

import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.tdf.common.HashUtil;
import org.tdf.common.serialize.Codec;
import org.tdf.common.serialize.Codecs;

@RunWith(JUnit4.class)
public class TrieImplTest extends AbstractTrieTest {
    Trie<String, String> newStringTrie() {
        return TrieImpl.newInstance(HashUtil::sha3, new AbstractTrieTest.NoDoubleDeleteStore(), Codecs.STRING, Codecs.STRING);
    }

    Trie<byte[], byte[]> newBytesTrie() {
        return TrieImpl.newInstance(HashUtil::sha3, new AbstractTrieTest.NoDoubleDeleteStore(), Codec.identity(), Codec.identity());
    }
}

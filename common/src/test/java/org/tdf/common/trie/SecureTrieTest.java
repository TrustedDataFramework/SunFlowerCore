package org.tdf.common.trie;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.tdf.common.TrieUtil;
import org.tdf.common.serialize.Codec;
import org.tdf.common.serialize.Codecs;
import org.tdf.common.store.ByteArrayMapStore;
import org.tdf.common.store.NoDeleteStore;
import org.tdf.common.util.HashUtil;
import org.tdf.common.util.HexBytes;

@RunWith(JUnit4.class)
public class SecureTrieTest {

    private Trie<byte[], String> notSecured;

    private Trie<byte[], String> secured;


    @Before
    public void before() {
        notSecured = TrieUtil.<byte[], String>builder()
            .keyCodec(Codec.identity())
            .valueCodec(Codecs.STRING)
            .store(new NoDeleteStore<>(new ByteArrayMapStore<>(), x -> x == null || x.length == 0))
            .build();

        secured = new SecureTrie<>(notSecured);
    }

    @Test
    public void test() {
        secured.set("1".getBytes(), "1");
        assert secured.getSize() > 0;
        assert secured.getSize() == 1;
        assert secured.get("1".getBytes()).equals("1");
    }


    @Test
    public void testRevert() {
        secured.set("1".getBytes(), "1");
        HexBytes root = secured.commit();
        secured.set("2".getBytes(), "2");
        HexBytes root2 = secured.commit();
        assert secured.revert(root).getSize() == 1;
        assert secured.revert(root).get("1".getBytes()).equals("1");
        assert secured.revert(root2).getSize() == 2;
        assert secured.revert(root2).get("1".getBytes()).equals("1");
        assert secured.revert(root2).get("2".getBytes()).equals("2");
    }
}

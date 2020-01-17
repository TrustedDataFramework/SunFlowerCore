package org.tdf.common.trie;

import com.google.common.hash.HashFunction;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.tdf.common.HashUtil;
import org.tdf.common.serialize.Codec;
import org.tdf.common.serialize.Codecs;
import org.tdf.common.store.ByteArrayMapStore;
import org.tdf.common.store.NoDeleteStore;

import java.util.Map;

@RunWith(JUnit4.class)
public class SecureTrieTest{

    private Trie<byte[], String> notSecured;

    private Trie<byte[], String> secured;


    @Before
    public void before(){
        notSecured = Trie.<byte[], String>builder()
                    .hashFunction(HashUtil::sha3)
                    .keyCodec(Codec.identity())
                    .valueCodec(Codecs.STRING)
                    .store(new NoDeleteStore<>(new ByteArrayMapStore<>()))
                .build();

        secured = new SecureTrie<>(notSecured, HashUtil::sha3);
    }

    @Test
    public void test() {
        secured.put("1".getBytes(), "1");
        assert !secured.isEmpty();
        assert secured.size() == 1;
        assert secured.get("1".getBytes()).get().equals("1");
    }

    @Test
    public void testAsMap() {
        Map<byte[], String> map = secured.asMap();
        map.put("1".getBytes(), "1");
        assert secured.get("1".getBytes()).get().equals("1");
        map.put("2".getBytes(), "2");
        assert !secured.isEmpty();
        assert secured.size() == 2;
        secured.keySet().remove("2".getBytes());
        assert map.size() == 1;
    }


    @Test
    public void testRevert(){
        secured.put("1".getBytes(), "1");
        byte[] root = secured.commit();
        secured.put("2".getBytes(), "2");
        byte[] root2 = secured.commit();
        assert secured.revert(root).size() == 1;
        assert secured.revert(root).get("1".getBytes()).get().equals("1");
        assert secured.revert(root2).size() == 2;
        assert secured.revert(root2).get("1".getBytes()).get().equals("1");
        assert secured.revert(root2).get("2".getBytes()).get().equals("2");
    }
}

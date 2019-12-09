package org.tdf.trie;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.spongycastle.util.encoders.Hex;
import org.tdf.common.HashUtil;
import org.tdf.common.Store;
import org.tdf.serialize.Serializers;
import org.tdf.store.ByteArrayMapStore;
import org.tdf.store.StoreWrapper;
import org.tdf.util.ByteArraySet;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.tdf.common.HashUtil.EMPTY_TRIE_HASH;

@RunWith(JUnit4.class)
public class TrieTest {
    private static String LONG_STRING = "1234567890abcdefghijklmnopqrstuvwxxzABCEFGHIJKLMNOPQRSTUVWXYZ";
    private static String ROOT_HASH_EMPTY = Hex.toHexString(EMPTY_TRIE_HASH);

    private static String c = "c";
    private static String ca = "ca";
    private static String cat = "cat";
    private static String dog = "dog";
    private static String doge = "doge";
    private static String test = "test";
    private static String dude = "dude";

    @Test
    public void test1() {
        TrieImpl trie = new TrieImpl(HashUtil::sha3, new ByteArrayMapStore<>());
        Arrays.asList("test", "toaster", "toasting", "slow", "slowly")
                .forEach(x -> trie.put(x.getBytes(), x.getBytes()));

        Set<byte[]> keys = trie.keySet();
        Collection<byte[]> values = trie.values();
        for (String s : Arrays.asList("test", "toaster", "toasting", "slow", "slowly")
        ) {
            assert keys.contains(s.getBytes());
            assert Arrays.equals(trie.get(s.getBytes()).get(), s.getBytes());
            assert values.contains(s.getBytes());
        }

        assert trie.size() == 5;
    }

    public interface StringTrie extends Store<String, String> {
    }

    @Test
    public void testDeleteShortString1() {
        String ROOT_HASH_BEFORE = "a9539c810cc2e8fa20785bdd78ec36cc1dab4b41f0d531e80a5e5fd25c3037ee";
        String ROOT_HASH_AFTER =  "fc5120b4a711bca1f5bb54769525b11b3fb9a8d6ac0b8bf08cbb248770521758";

        TrieImpl impl = new TrieImpl(HashUtil::sha3, new ByteArrayMapStore<>());
        Store<String, String> trie = new StoreWrapper<>(impl, Serializers.STRING, Serializers.STRING);

        trie.put(cat, dog);
        assertEquals(dog, new String(trie.get(cat).get()));

        trie.put(ca, dude);
        assertEquals(dude, new String(trie.get(ca).get()));
        assertEquals(ROOT_HASH_BEFORE, Hex.toHexString(impl.getRootHash()));
    }

    @Test
//    count n = 1000000 size trie 607ms
    public void test7() {
        boolean performance = false;
        if (!performance) return;
        TrieImpl trie = new TrieImpl(HashUtil::sha3, new ByteArrayMapStore<>());
        byte[] empty = new byte[0];
        SecureRandom sr = new SecureRandom();
        Set<byte[]> set = new ByteArraySet();
        for (int i = 0; i < 1_000_000; i++) {
            byte[] bytes = new byte[32];
            sr.nextBytes(bytes);
            set.add(bytes);
            trie.put(bytes, empty);
        }
        long start = System.currentTimeMillis();
        int size = trie.size();
        long end = System.currentTimeMillis();
        assert size == set.size();
        System.out.println("count size at " + size + " " + (end - start) + " ms");
    }
}

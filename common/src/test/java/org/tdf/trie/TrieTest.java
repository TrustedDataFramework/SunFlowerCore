package org.tdf.trie;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.tdf.util.ByteArraySet;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

@RunWith(JUnit4.class)
public class TrieTest {

    @Test
    public void test1() {
        TrieImpl trie = new TrieImpl();
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

    @Test
//    count n = 1000000 size trie 607ms
    public void test7() {
        boolean performance = false;
        if (!performance) return;
        TrieImpl trie = new TrieImpl();
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

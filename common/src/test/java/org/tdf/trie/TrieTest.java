package org.tdf.trie;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

@RunWith(JUnit4.class)
public class TrieTest {

    @Test
    public void test1(){
        TrieImpl trie = new TrieImpl();
        Arrays.asList("test", "toaster", "toasting", "slow", "slowly")
                .forEach(x -> trie.put(x.getBytes(), x.getBytes()));

        Set<byte[]> keys = trie.keySet();
        Collection<byte[]> values = trie.values();
        for (String s : Arrays.asList("test", "toaster", "toasting", "slow", "slowly")
        ) {
            assert keys.contains(s.getBytes());
            assert values.contains(s.getBytes());
        }

        assert trie.size() == 5;
    }
}

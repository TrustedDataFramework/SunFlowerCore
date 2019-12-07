package org.tdf.trie;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Arrays;

@RunWith(JUnit4.class)
public class NodeTest {

    @Test
    public void test1() {
        Node n = Node.newLeaf(TrieKey.fromNormal("test".getBytes()), "test".getBytes());
        Arrays.asList("toaster", "toasting", "slow", "slowly")
                .forEach(x -> n.insert(TrieKey.fromNormal(x.getBytes()), x.getBytes()));

        for (String s : Arrays.asList("toaster", "toasting", "slow", "slowly")
        ) {
            assert Arrays.equals(n.get(TrieKey.fromNormal(s.getBytes())), s.getBytes());
        }
    }
}

package org.tdf.trie;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

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

    @Test
    public void test2() {
        Node n = Node.newLeaf(TrieKey.fromNormal("do".getBytes()), "verb".getBytes());
        List<String> li = Arrays.asList("dog", "puppy", "doge", "coin", "horse", "stallion");
        for (int i = 0; i < li.size(); i += 2) {
            String key = li.get(i);
            String val = li.get(i + 1);
            n.insert(TrieKey.fromNormal(key.getBytes(StandardCharsets.US_ASCII)), val.getBytes(StandardCharsets.US_ASCII));
        }
        for (int i = 0; i < li.size(); i += 2) {
            String key = li.get(i);
            String val = li.get(i + 1);
            assert Arrays.equals(n.get(TrieKey.fromNormal(key.getBytes(StandardCharsets.US_ASCII))), val.getBytes(StandardCharsets.US_ASCII));
        }
    }

    @Test
    public void test3() {
        Node n = Node.newLeaf(TrieKey.fromNormal("do".getBytes()), "verb".getBytes());
        List<String> li = Arrays.asList("dog", "puppy", "doge", "coin", "horse", "stallion");
        for (int i = 0; i < li.size(); i += 2) {
            String key = li.get(i);
            String val = li.get(i + 1);
            n.insert(TrieKey.fromNormal(key.getBytes()), val.getBytes());
        }
        n = n.delete(TrieKey.fromNormal("doge".getBytes()));
        assert Arrays.equals(n.get(TrieKey.fromNormal("dog".getBytes())), "puppy".getBytes());
        assert Arrays.equals(n.get(TrieKey.fromNormal("do".getBytes())), "verb".getBytes());
        assert Arrays.equals(n.get(TrieKey.fromNormal("horse".getBytes())), "stallion".getBytes());
        assert n.get(TrieKey.fromNormal("doge".getBytes())) == null;
    }

    @Test
    public void test4() {
        Node n = Node.newLeaf(TrieKey.fromNormal("do".getBytes()), "verb".getBytes());
        List<String> li = Arrays.asList("dog", "puppy", "doge", "coin", "horse", "stallion");
        for (int i = 0; i < li.size(); i += 2) {
            String key = li.get(i);
            String val = li.get(i + 1);
            n.insert(TrieKey.fromNormal(key.getBytes()), val.getBytes());
        }
        n = n.delete(TrieKey.fromNormal("do".getBytes()));
        assert Arrays.equals(n.get(TrieKey.fromNormal("dog".getBytes())), "puppy".getBytes());
        assert Arrays.equals(n.get(TrieKey.fromNormal("doge".getBytes())), "coin".getBytes());
        assert Arrays.equals(n.get(TrieKey.fromNormal("horse".getBytes())), "stallion".getBytes());
        assert n.get(TrieKey.fromNormal("do".getBytes())) == null;
    }

    @Test
    public void test5() {
        Node n = Node.newLeaf(TrieKey.fromNormal("do".getBytes()), "verb".getBytes());
        List<String> li = Arrays.asList("dog", "puppy", "doge", "coin", "horse", "stallion");
        for (int i = 0; i < li.size(); i += 2) {
            String key = li.get(i);
            String val = li.get(i + 1);
            n.insert(TrieKey.fromNormal(key.getBytes(StandardCharsets.US_ASCII)), val.getBytes(StandardCharsets.US_ASCII));
        }
        for (int i = 0; i < li.size(); i += 2) {
            String key = li.get(i);
            String val = li.get(i + 1);
            n = n.delete(TrieKey.fromNormal(key.getBytes()));
        }
        n = n.delete(TrieKey.fromNormal("do".getBytes()));
        assert n == null;
    }

    @Test
    public void test6(){
        Node n = Node.newLeaf(TrieKey.single(3).concat(TrieKey.single(15)).concat(TrieKey.single(0x0e)), "abc".getBytes());
        n.insert(TrieKey.fromNormal(new byte[]{0x3f, 0x3d, 0x4a}), "dog".getBytes());
        n.insert(TrieKey.fromNormal(new byte[]{0x3f, 0x3d, 0x4b}), "dog1".getBytes());
        assert Arrays.equals(n.get(TrieKey.fromNormal(new byte[]{0x3f, 0x3d, 0x4a})), "dog".getBytes());
        assert Arrays.equals(n.get(TrieKey.fromNormal(new byte[]{0x3f, 0x3d, 0x4b})), "dog1".getBytes());

        n = n.delete(TrieKey.fromNormal(new byte[]{0x3f, 0x3d, 0x4b}));
        assert n.get(TrieKey.fromNormal(new byte[]{0x3f, 0x3d, 0x4b})) == null;
    }
}

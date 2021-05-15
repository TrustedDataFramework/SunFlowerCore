package org.tdf.common.trie;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.tdf.common.serialize.Codec;
import org.tdf.common.store.ByteArrayMapStore;
import org.tdf.common.store.Store;
import org.tdf.common.util.FastByteComparisons;
import org.tdf.common.util.HashUtil;
import org.tdf.common.util.HexBytes;
import org.tdf.rlp.RLPElement;
import org.tdf.rlp.RLPList;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RunWith(JUnit4.class)
public abstract class ProofTest {
    private Trie<String, String> trie;

    private String paramnesia = "paramnesia";

    private String stoopingly = "stoopingly";

    private List<String> proofKeys = Arrays.asList(
        "demoralize", "estivating", "impanelled", "acrogynous", "hamantasch", "prepotency"
    );

    private int fileSize;

    abstract Trie<String, String> supplyTrie();

    @Before
    public void before() throws Exception {
        trie = supplyTrie();

        URL url = ClassLoader
            .getSystemResource("trie/massive-upload.dmp");
        fileSize = url.openStream().available();
        Files.lines(Paths.get(url.toURI()))
            .forEach(s -> {
                String[] kv = s.split("=");
                if (kv[0].equals("*") || kv[0].equals(stoopingly)) return;
                trie.set(kv[0], kv[1]);
            });

        trie.commit();
    }


    @Test
    public void test() {
        HexBytes root = trie.getRootHash();

        Map<HexBytes, HexBytes> merklePath = trie.getProof(paramnesia);
        String val = trie.get(paramnesia);

        Store<byte[], byte[]> store = new ByteArrayMapStore<>();
        merklePath.forEach((k, v) -> store.set(k.getBytes(), v.getBytes()));

        assert trie
            .revert(root, store)
            .get(paramnesia)
            .equals(val);

        merklePath = trie.getProof(stoopingly);
        Store<byte[], byte[]> store2 = new ByteArrayMapStore<>();
        merklePath.forEach((k, v) -> store2.set(k.getBytes(), v.getBytes()));

        assert trie.revert(root, store2).get(stoopingly) == null;

        System.out.println("file size = " + fileSize);
        System.out.println("proof size = " + getProofSize(merklePath));
    }

    @Test
    public void testEmpty() {
        Trie<String, String> empty = trie.revert();
        Map<HexBytes, HexBytes> merklePath = empty.getProof(stoopingly);

        Store<byte[], byte[]> store = new ByteArrayMapStore<>();
        merklePath.forEach((k, v) -> store.set(k.getBytes(), v.getBytes()));

        HexBytes root = trie.revert(trie.getNullHash(), store).getRootHash();
        assert
            empty.getNullHash().equals(
                root
            );
    }

    @Test
    public void testMultiKeys() {
        Map<HexBytes, HexBytes> rlpElement = trie.getProof(proofKeys);

        System.out.println(
            "proof size = " + getProofSize(rlpElement)
        );

        Store<byte[], byte[]> store = new ByteArrayMapStore<>();
        rlpElement.forEach((k, v) -> store.set(k.getBytes(), v.getBytes()));

        Trie<String, String> merkleProof =
            trie.revert(trie.getRootHash(), store);

        for (String k : proofKeys) {
            String actual = merkleProof.get(k);
            String expected = trie.get(k);
            assert actual.equals(expected);
        }
    }



    protected int getProofSize(Map<HexBytes, HexBytes> proof) {
        return proof.entrySet().stream().map(e -> e.getKey().size() + e.getValue().size())
            .reduce(0, Integer::sum);
    }
}

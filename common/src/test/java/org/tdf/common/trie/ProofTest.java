package org.tdf.common.trie;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.tdf.common.util.FastByteComparisons;
import org.tdf.common.util.HexBytes;
import org.tdf.rlp.RLPElement;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

@RunWith(JUnit4.class)
public abstract class ProofTest {
    private Trie<String, String> trie;

    private String paramnesia = "paramnesia";

    private String stoopingly = "stoopingly";

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
                    trie.put(kv[0], kv[1]);
                });

        trie.commit();
    }


    @Test
    public void test() {
        byte[] root = trie.getRootHash();

        RLPElement merklePath = trie.getProof(paramnesia);
        String val = trie.get(paramnesia).get();

        assert HexBytes.fromBytes(trie.revertToProof(merklePath).getRootHash())
                .equals(HexBytes.fromBytes(root));

        assert trie
                .revertToProof(merklePath)
                .get(paramnesia).get()
                .equals(val);

        merklePath = trie.getProof(stoopingly);

        assert HexBytes.fromBytes(trie.revertToProof(merklePath).getRootHash())
                .equals(HexBytes.fromBytes(root));

        assert !trie.revertToProof(merklePath).containsKey(stoopingly);

        System.out.println(fileSize);
        System.out.println(merklePath.getEncoded().length);
    }

    @Test
    public void testEmpty(){
        Trie<String, String> empty = trie.revert();
        RLPElement merklePath = empty.getProof(stoopingly);
        byte[] root = trie.revertToProof(merklePath).getNullHash();
        assert FastByteComparisons.equal(
                empty.getNullHash(),
                root
        );
    }
}

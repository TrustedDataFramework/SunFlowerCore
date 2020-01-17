package org.tdf.common.trie;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.tdf.common.HashUtil;
import org.tdf.common.serialize.Codecs;
import org.tdf.common.store.ByteArrayMapStore;
import org.tdf.common.util.FastByteComparisons;
import org.tdf.common.util.HexBytes;
import org.tdf.rlp.RLPElement;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

@RunWith(JUnit4.class)
public class ProofTest {
    private Trie<String, String> trie;

    private String paramnesia = "paramnesia";

    private String stoopingly = "stoopingly";

    private int fileSize;

    @Before
    public void before() throws Exception {
        trie = Trie.<String, String>builder()
                .store(new ByteArrayMapStore<>())
                .valueCodec(Codecs.STRING)
                .keyCodec(Codecs.STRING)
                .hashFunction(HashUtil::sha3)
                .build();

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

        RLPElement merklePath = trie.getMerklePath(paramnesia);
        String val = trie.get(paramnesia).get();

        assert HexBytes.fromBytes(trie.fromMerklePath(merklePath).commit())
                .equals(HexBytes.fromBytes(root));

        assert trie
                .fromMerklePath(merklePath)
                .get(paramnesia).get()
                .equals(val);

        merklePath = trie.getMerklePath(stoopingly);

        assert HexBytes.fromBytes(trie.fromMerklePath(merklePath).commit())
                .equals(HexBytes.fromBytes(root));

        assert !trie.fromMerklePath(merklePath).containsKey(stoopingly);

        System.out.println(fileSize);
        System.out.println(merklePath.getEncoded().length);
    }

    @Test
    public void testEmpty(){
        Trie<String, String> empty = trie.revert();
        RLPElement merklePath = empty.getMerklePath(stoopingly);
        byte[] root = trie.fromMerklePath(merklePath).commit();
        assert FastByteComparisons.equal(
                empty.getNullHash(),
                root
        );
    }
}

package org.tdf.common.trie;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.tdf.common.HashUtil;
import org.tdf.common.serialize.Codec;
import org.tdf.common.store.ByteArrayMapStore;
import org.tdf.common.store.MapStore;
import org.tdf.common.util.FastByteComparisons;
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
                    trie.put(kv[0], kv[1]);
                });

        trie.commit();
    }


    @Test
    public void test() {
        byte[] root = trie.getRootHash();

        Map<byte[], byte[]> merklePath = trie.getProof(paramnesia);
        String val = trie.get(paramnesia);

        assert trie
                .revert(root, new MapStore<>(merklePath))
                .get(paramnesia)
                .equals(val);

        merklePath = trie.getProof(stoopingly);


        assert trie.revert(root, new MapStore<>(merklePath)).get(stoopingly) == null;

        System.out.println("file size = " + fileSize);
        System.out.println("proof size = " + getProofSize(merklePath));
    }

    @Test
    public void testEmpty() {
        Trie<String, String> empty = trie.revert();
        Map<byte[], byte[]> merklePath = empty.getProof(stoopingly);
        byte[] root = trie.revert(trie.getNullHash(), new MapStore<>(merklePath)).getRootHash();
        assert FastByteComparisons.equal(
                empty.getNullHash(),
                root
        );
    }

    @Test
    public void testMultiKeys() {
        Map<byte[], byte[]> rlpElement = trie.getProof(proofKeys);

        System.out.println(
                "proof size = " + getProofSize(rlpElement)
        );

        Trie<String, String> merkleProof =
                trie.revert(trie.getRootHash(), new MapStore<>(rlpElement));

        for (String k : proofKeys) {
            String actual = merkleProof.get(k);
            String expected = trie.get(k);
            assert actual.equals(expected);
        }
    }


    // TODO: reduce proof size
    @Ignore
    @Test
    public void testPublicChainData() throws Exception {
        String path = "C:\\Users\\Sal\\Desktop\\dumps\\blocks\\genesis.800040.rlp";

        Trie<byte[], RLPElement> accountTrie =
                Trie.<byte[], RLPElement>builder()
                        .hashFunction(HashUtil::sha3)
                        .keyCodec(Codec.identity())
                        .valueCodec(
                                Codec.newInstance(RLPElement::getEncoded, RLPElement::fromEncoded)
                        )
                        .store(new ByteArrayMapStore<>())
                        .build();

        byte[] binaryFile = Files.readAllBytes(Paths.get(path));

        System.out.println("file size = " + binaryFile.length);

        RLPElement el =
                RLPElement.fromEncoded(binaryFile);

        RLPList accounts = el.get(1).asRLPList();

        List<byte[]> bigAccounts =
                accounts
                        .stream().sorted((x, y) -> y.getEncoded().length - x.getEncoded().length)
                        .limit(100)
                        .map(a -> a.get(0).get(1).asBytes())
                        .collect(Collectors.toList());

        for (RLPElement account : accounts) {
            byte[] key = account.get(0).get(1).asBytes();
            accountTrie.put(key, account);
        }

        byte[] root = accountTrie.commit();

        Map<byte[], byte[]> proof = accountTrie.getProof(bigAccounts);

        Trie<byte[], RLPElement> proofTrie = accountTrie.revert(root, new MapStore<>(proof));

        for (byte[] bigAccount : bigAccounts) {
            assert FastByteComparisons.equal(proofTrie.get(bigAccount)
                    .getEncoded(), accountTrie.get(bigAccount).getEncoded());
        }

        System.out.println("proof size = " + getProofSize(proof));

        System.out.println("accounts size = " +
                bigAccounts.stream().map(accountTrie::get)
                        .map(e -> e.getEncoded().length)
                        .reduce(0, Integer::sum)
        );

        System.out.println("proofs size = " + bigAccounts.stream()
                .map(Collections::singleton)
                .map(accountTrie::getProof)
                .map(this::getProofSize)
                .reduce(0, Integer::sum)
        );

    }

    protected int getProofSize(Map<byte[], byte[]> proof) {
        return proof.entrySet().stream().map(e -> e.getKey().length + e.getValue().length)
                .reduce(0, Integer::sum);
    }
}

package org.tdf.common.trie;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.tdf.common.HashUtil;
import org.tdf.common.serialize.Codec;
import org.tdf.common.store.ByteArrayMapStore;
import org.tdf.common.util.ByteArrayMap;
import org.tdf.common.util.FastByteComparisons;
import org.tdf.common.util.HexBytes;
import org.tdf.rlp.RLPElement;
import org.tdf.rlp.RLPList;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
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
    public void testEmpty() {
        Trie<String, String> empty = trie.revert();
        RLPElement merklePath = empty.getProof(stoopingly);
        byte[] root = trie.revertToProof(merklePath).getNullHash();
        assert FastByteComparisons.equal(
                empty.getNullHash(),
                root
        );
    }

    @Test
    public void testMultiKeys() {
        RLPElement rlpElement = trie.getProof(proofKeys);
        System.out.println(rlpElement.getEncoded().length);

        Trie<String, String> merkleProof =
                trie.revertToProof(rlpElement);

        for (String k : proofKeys) {
            Optional<String> actual = merkleProof.get(k);
            Optional<String> expected = trie.get(k);
            assert (!actual.isPresent() && !expected.isPresent()) ||
                    actual.get().equals(expected.get());
        }
    }


    // TODO: reduce proof size
    @Ignore
    @Test
    public void testPublicChainData() throws Exception {
        String path = "C:\\Users\\Sal\\Desktop\\dumps\\genesis\\genesis.800040.rlp";

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
                .stream().sorted((x, y) -> y.getEncoded().length - x.getEncoded().length )
                .limit(100)
                .map(a -> a.get(0).get(1).asBytes())
                .collect(Collectors.toList());

        for (RLPElement account : accounts) {
            byte[] key = account.get(0).get(1).asBytes();
            accountTrie.put(key, account);
        }

        byte[] root = accountTrie.commit();

        RLPElement proof = accountTrie.getProof(bigAccounts);

        System.out.println("proof size = " + proof.getEncoded().length);

        System.out.println("accounts size = " + bigAccounts.stream().map(x -> x.length).reduce(0, Integer::sum));
        System.out.println("proof size = " + bigAccounts.stream()
                .map(Collections::singleton)
                .map(accountTrie::getProof)
                .map(e -> e.getEncoded().length)
                .reduce(0, Integer::sum)
        );

    }
}

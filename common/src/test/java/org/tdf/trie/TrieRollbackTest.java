package org.tdf.trie;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.spongycastle.util.encoders.Hex;
import org.tdf.common.HashUtil;
import org.tdf.serialize.Codecs;
import org.tdf.store.ByteArrayMapStore;
import org.tdf.store.NoDeleteStore;
import org.tdf.store.Store;

import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

@RunWith(JUnit4.class)
public class TrieRollbackTest {
    protected NoDeleteStore<byte[], byte[]> database;
    protected Trie<String, String> trie;
    protected List<byte[]> roots;
    protected Map<String, Map<String, String>> dumps;
    protected List<Set<byte[]>> nodes;

    @Before
    public void before() throws Exception {
        database = new NoDeleteStore<>(new ByteArrayMapStore<>(), new ByteArrayMapStore<>());

        trie = TrieImpl.newInstance(HashUtil::sha3, database, Codecs.STRING, Codecs.STRING);

        roots = new ArrayList<>();

        dumps = new HashMap<>();

        nodes = new ArrayList<>();

        URL massiveUpload_1 = ClassLoader
                .getSystemResource("trie/massive-upload.dmp");

        File file = new File(massiveUpload_1.toURI());
        List<String> strData = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);


        for (int i = 0; i < 100; ++i) {

            String[] keyVal = strData.get(i).split("=");

            if (keyVal[0].equals("*"))
                trie.remove(keyVal[1].trim());
            else
                trie.put(keyVal[0].trim(), keyVal[1].trim());

            byte[] rootHash = trie.commit();
            trie.flush();
            roots.add(rootHash);
            nodes.add(trie.dump());
            dumps.put(Hex.toHexString(rootHash), dump(trie));
        }
    }

    // rollback successful
    @Test
    public void test1() {
        for (byte[] rootHash : roots) {
            trie = trie.moveTo(rootHash, database);
            assert equals(dump(trie), dumps.get(Hex.toHexString(rootHash)));
        }
    }

    // rollback failed if later trie had been flushed
    @Test
    public void test2() {
        database.compact();
        for (int i = 0; i < roots.size() - 1; i++) {
            Exception e = null;
            try{
                trie.moveTo(roots.get(i), database);
            }catch (Exception ex){
                e = ex;
            }
            assert e != null;
            e = null;
        }
        trie.moveTo(roots.get(roots.size() - 1), database);
    }

    private Map<String, String> dump(Store<String, String> store) {
        Map<String, String> m = new HashMap<>();
        store.keySet().forEach(x -> m.put(x, store.get(x).get()));
        return m;
    }

    private boolean equals(Map<String, String> m1, Map<String, String> m2) {
        if (m1.size() != m2.size()) return false;
        for (String k : m1.keySet()) {
            String v1 = m1.get(k);
            if (!m2.containsKey(k)) return false;
            if (!v1.equals(m2.get(k))) return false;
        }
        for (String k : m2.keySet()) {
            String v2 = m2.get(k);
            if (!m1.containsKey(k)) return false;
            if (!v2.equals(m1.get(k))) return false;
        }
        return true;
    }
}

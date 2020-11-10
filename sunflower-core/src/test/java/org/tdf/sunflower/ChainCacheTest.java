package org.tdf.sunflower;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.core.io.ClassPathResource;
import org.tdf.common.store.Store;
import org.tdf.common.types.Chained;
import org.tdf.common.util.HexBytes;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RunWith(JUnit4.class)
// set env WRAPPER=true to test wrapper method
public class ChainCacheTest {
    private static final DatabaseConfig PROPERTIES;
    private static Store<byte[], byte[]> PERSISTENT;

    static {
        PROPERTIES = new DatabaseConfig();
        PROPERTIES.setName("rocksdb");
        PROPERTIES.setDirectory("local");
        PROPERTIES.setMaxOpenFiles(512);
        try {
//            PERSISTENT = new PersistentDataStoreFactory(PROPERTIES).create("chain-cache");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<Node> getCache() throws Exception {
        InputStream in = new ClassPathResource("nodes.jsonc").getInputStream();
        JsonNode[][] json = Start.MAPPER.readValue(in, JsonNode[][].class);
        List<Node> ret = new ArrayList<>();
        for (JsonNode[] nodes : json) {
            HexBytes hash = HexBytes.fromHex(nodes[1].asText());
            HexBytes hashPrev = HexBytes.fromHex(nodes[2].asText());
            long height = nodes[0].asLong();
            ret.add(new Node(hash, hashPrev, height));
        }
        return ret;
    }

    @Test
    public void testGetDescendants() throws Exception {
        List<Node> cache = getCache();
        List<Node> descendents = Chained.getDescendentsOf(cache, HexBytes.fromHex("0000"));
        assert descendents.size() == 9;
        assert Chained.getDescendentsOf(cache, HexBytes.fromHex("0001")).size() == 7;
        Set<String> nodes = Chained.getDescendentsOf(cache, HexBytes.fromHex("0102"))
                .stream().map(n -> n.hash.toString()).collect(Collectors.toSet());
        assert nodes.size() == 3;
        assert nodes.containsAll(Arrays.asList("0203", "0103", "0204"));
    }

    @Test
    public void testGetAncestors() throws Exception {
        List<Node> cache = getCache();
        Set<String> ancestors = Chained.getAncestorsOf(
                cache, HexBytes.fromHex("0204")
        ).stream().map(n -> n.hash.toString()).collect(Collectors.toSet());

        assert ancestors.size() == 4;
        assert ancestors.containsAll(Arrays.asList("0203", "0102", "0001", "0000"));


        ancestors = Chained.getAncestorsOf(cache, HexBytes.fromHex("0000")).stream()
                .map(n -> n.hash.toString()).collect(Collectors.toSet());

        assert ancestors.size() == 0;
    }

    @Test
    public void testGetInitials() throws Exception {
        List<Node> initials = Chained.getInitialsOf(getCache());
        assert initials.size() == 1;
        assert initials.get(0).hash.toString().equals("0000");
    }

    @Test
    public void testGetLeaves() throws Exception {
        Set<String> leaves = Chained.getLeavesOf(getCache()).stream()
                .map(x -> x.hash.toString()).collect(Collectors.toSet());

        assert leaves.size() == 4;
        assert leaves.containsAll(Arrays.asList("0401", "0204", "0103", "0004"));
    }

    @Test
    public void testGetForks() throws Exception {
        System.out.print(Start.MAPPER.writeValueAsString(
                Chained.getForks(
                        getCache()
                )
        ));
    }

    public static class Node implements Chained {
        private HexBytes hash;
        private HexBytes hashPrev;
        private long height;

        public Node() {
        }

        public Node(HexBytes hash, HexBytes hashPrev, long height) {
            this.hash = hash;
            this.hashPrev = hashPrev;
            this.height = height;
        }

        @Override
        public HexBytes getHash() {
            return hash;
        }

        @Override
        public HexBytes getHashPrev() {
            return hashPrev;
        }

        public long getHeight() {
            return height;
        }
    }
}

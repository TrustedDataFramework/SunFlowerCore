package org.tdf.common;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.tdf.common.types.Chained;
import org.tdf.common.util.ChainCache;
import org.tdf.common.util.ChainCacheImpl;
import org.tdf.common.util.HexBytes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@RunWith(JUnit4.class)
// set env WRAPPER=true to test wrapper method
public class ChainCacheTest {

    public static ChainCache<Node> getCache(int sizeLimit) throws Exception {

        Node genesis = new Node(HexBytes.fromHex("0000"), HexBytes.fromHex("ffff"), 0);
        List<String> hashes = Arrays.asList("0001", "0002", "0003", "0004", "0005");
        List<Node> chain0 = new ArrayList<>();
        for (int i = 0; i < hashes.size(); i++) {
            if (i == 0) {
                chain0.add(new Node(
                        HexBytes.fromHex(hashes.get(i)), HexBytes.fromHex("0000"), Long.parseLong(hashes.get(i)))
                );
                continue;
            }
            chain0.add(new Node(
                            HexBytes.fromHex(hashes.get(i)), HexBytes.fromHex(hashes.get(i - 1)),
                            Long.parseLong(hashes.get(i).substring(2))
                    )
            );
        }
        hashes = Arrays.asList("0102", "0103", "0104", "0105");
        List<Node> chain1 = new ArrayList<>();
        for (int i = 0; i < hashes.size(); i++) {
            if (i == 0) {
                chain1.add(new Node(
                                HexBytes.fromHex(hashes.get(i)), HexBytes.fromHex("0001"),
                                Long.parseLong(hashes.get(i).substring(2))
                        )
                );
                continue;
            }
            chain1.add(new Node(
                            HexBytes.fromHex(hashes.get(i)), HexBytes.fromHex(hashes.get(i - 1)),
                            Long.parseLong(hashes.get(i).substring(2))
                    )
            );
        }
        hashes = Arrays.asList("0204", "0205", "0206");
        List<Node> chain2 = new ArrayList<>();
        for (int i = 0; i < hashes.size(); i++) {
            if (i == 0) {
                chain2.add(new Node(
                        HexBytes.fromHex(hashes.get(i)), HexBytes.fromHex("0103"),
                        Long.parseLong(hashes.get(i).substring(2))
                ));
                continue;
            }
            chain2.add(new Node(
                    HexBytes.fromHex(hashes.get(i)), HexBytes.fromHex(hashes.get(i - 1)),
                    Long.parseLong(hashes.get(i).substring(2))

            ));
        }
        ChainCacheImpl<Node> cache = new ChainCacheImpl<>(sizeLimit, Comparator.comparingLong(Node::getHeight));
        cache.add(genesis);
        cache.addAll(chain0);
        cache.addAll(chain1);
        cache.addAll(chain2);
        return cache;
    }

    @Test
    public void test() {
    }

    public static class Node implements Chained {
        private HexBytes hash;
        private HexBytes hashPrev;
        private long height;

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

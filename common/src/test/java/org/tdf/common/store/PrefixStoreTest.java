package org.tdf.common.store;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.tdf.common.serialize.Codecs;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;

@RunWith(JUnit4.class)
public class PrefixStoreTest {

    @Test
    public void test0() {
        Store<byte[], byte[]> s = new ByteArrayMapStore<>();
        PrefixStore<String, String> p = new PrefixStore<>(
                s,
                "aaa".getBytes(StandardCharsets.US_ASCII),
                Codecs.newRLPCodec(String.class),
                Codecs.newRLPCodec(String.class)
        );

        p.put("aaa", "bbb");
//        Map<String, String> m = new HashMap<>(p.asMap());
        assertEquals("bbb", p.get("aaa"));
    }
}

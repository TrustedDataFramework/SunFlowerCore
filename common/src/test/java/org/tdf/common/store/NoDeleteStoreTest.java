package org.tdf.common.store;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;

@Ignore
public abstract class NoDeleteStoreTest {
    protected Store<byte[], byte[]> store;


    abstract protected Store<byte[], byte[]> supplyNoDelete();

    @Before
    public void before() {
        store = supplyNoDelete();
        store.put("a".getBytes(), "1".getBytes());
        store.put("b".getBytes(), "2".getBytes());
        store.put("c".getBytes(), "3".getBytes());
        store.flush();
    }


    @Test
    public void test1() {
        store.remove("a".getBytes());
        store.flush();
        assert store.containsKey("a".getBytes());
        assert store.get("a".getBytes()).map(x -> Arrays.equals(x, "1".getBytes())).orElse(false);
        store.flush();
        assert store.containsKey("a".getBytes());
    }

    @Test
    public void test2() {
        store.remove("a".getBytes());
        store.put("a".getBytes(), "11".getBytes());
        assert Arrays.equals(store.get("a".getBytes()).get(), "11".getBytes());
    }
}

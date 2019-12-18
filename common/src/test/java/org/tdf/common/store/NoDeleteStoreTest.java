package org.tdf.common.store;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Arrays;

@RunWith(JUnit4.class)
public class NoDeleteStoreTest {
    protected NoDeleteStore<byte[], byte[]> store;

    protected Store<byte[], byte[]> delegated;

    private Store<byte[], byte[]> deleted;

    @Before
    public void before(){
        delegated = new ByteArrayMapStore<>();
        deleted = new ByteArrayMapStore<>();
        store = new NoDeleteStore<>(delegated, deleted);
        store.put("a".getBytes(), "1".getBytes());
        store.put("b".getBytes(), "2".getBytes());
        store.put("c".getBytes(), "3".getBytes());
    }


    @Test
    public void test1(){
        store.remove("a".getBytes());
        assert store.containsKey("a".getBytes());
        assert store.get("a".getBytes()).map(x -> Arrays.equals(x, "1".getBytes())).orElse(false);
        store.flush();
        assert store.containsKey("a".getBytes());
        store.compact();
        assert !store.containsKey("a".getBytes());
    }

    @Test
    public void test2(){
        store.remove("a".getBytes());
        store.put("a".getBytes(), "11".getBytes());
        assert Arrays.equals(store.get("a".getBytes()).get(), "11".getBytes());
    }
}

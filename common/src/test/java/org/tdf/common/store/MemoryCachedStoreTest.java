package org.tdf.common.store;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.tdf.common.util.ByteArraySet;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@RunWith(JUnit4.class)
public class MemoryCachedStoreTest {
    protected MemoryCachedStore<byte[]> store;

    protected Store<byte[], byte[]> delegated;

    @Before
    public void before(){
        delegated = new ByteArrayMapStore<>();
        store = new MemoryCachedStore<>(delegated);
    }

    @Test
    public void test1(){
        store.put("a".getBytes(), "b".getBytes());
        assert store.containsKey("a".getBytes());
        assert Arrays.equals(store.get("a".getBytes()).get(), "b".getBytes());
        assert !delegated.containsKey("a".getBytes());
    }

    @Test
    public void test2(){
        delegated.put("a".getBytes(), "b".getBytes());
        assert store.containsKey("a".getBytes());
        assert Arrays.equals(store.get("a".getBytes()).get(), "b".getBytes());
    }

    @Test
    public void test3(){
        delegated.put("a".getBytes(), "b".getBytes());
        store.putIfAbsent("a".getBytes(), "c".getBytes());
        assert Arrays.equals(store.get("a".getBytes()).get(), "b".getBytes());
        store.putIfAbsent("b".getBytes(), "c".getBytes());
        assert Arrays.equals(store.get("b".getBytes()).get(), "c".getBytes());
    }

    @Test
    public void test4(){
        delegated.put("a".getBytes(), "b".getBytes());
        store.remove("a".getBytes());
        assert delegated.containsKey("a".getBytes());
        assert !store.containsKey("a".getBytes());
        assert store.deleted.containsKey("a".getBytes());
    }

    @Test
    public void test5(){
        delegated.put("a".getBytes(), "b".getBytes());
        store.put("a".getBytes(), "c".getBytes());
        store.remove("a".getBytes());
        assert !store.containsKey("a".getBytes());
        assert delegated.containsKey("a".getBytes());
    }

    @Test
    public void test6(){
        store.flush();
        assert store.isEmpty();
        assert delegated.isEmpty();
    }

    @Test
    public void test7(){
        store.put("a".getBytes(), "c".getBytes());
        store.remove("a".getBytes());
        store.flush();
        assert store.isEmpty();
        assert delegated.isEmpty();
    }

    @Test
    public void test8(){
        store.put("a".getBytes(), "c".getBytes());
        store.put("b".getBytes(), "c".getBytes());
        store.remove("a".getBytes());
        store.flush();
        assert !store.isEmpty();
        assert delegated.size() == 1;
        assert Arrays.equals(delegated.get("b".getBytes()).get(), "c".getBytes());
    }

    @Test
    public void test9(){
        store.put("a".getBytes(), "c".getBytes());
        store.put("b".getBytes(), "c".getBytes());
        store.remove("a".getBytes());
        store.flush();
        store.clear();
        store.flush();
        assert delegated.isEmpty();
    }

    @Test
    public void test10(){
        store.put("a".getBytes(), "c".getBytes());
        store.put("b".getBytes(), "c".getBytes());
        store.remove("a".getBytes());
        assert store.size() == 1;
        assert store.stream()
                        .map(Map.Entry::getValue)
                        .collect(Collectors.toCollection(ByteArraySet::new))
                        .contains("c".getBytes());
    }

    @Test
    public void test11(){
        delegated.put("a".getBytes(), "f".getBytes());
        delegated.put("c".getBytes(), "f".getBytes());

        store.put("a".getBytes(), "c".getBytes());
        store.put("b".getBytes(), "c".getBytes());
        store.remove("b".getBytes());
        assert store.size() == 2;
        assert store.stream()
                .map(Map.Entry::getValue)
                .collect(Collectors.toCollection(ByteArraySet::new))
                .contains("c".getBytes());
    }
}

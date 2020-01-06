package org.tdf.common.store;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.tdf.common.serialize.Codecs;
import org.tdf.common.util.ByteArrayMap;

import java.util.Map;

@Ignore
public abstract class DBTests {

    protected DatabaseStore databaseStore;

    protected Store<String, String> wrapped;

    abstract DatabaseStore getDB();

    @Before
    public void before() {
        databaseStore = getDB();
        databaseStore.init(DBSettings.DEFAULT);
        wrapped = new StoreWrapper<>(databaseStore, Codecs.STRING, Codecs.STRING);
    }

    @After
    public void after() {
        databaseStore.clear();
        databaseStore.close();
    }

    @Test
    public void test() {
        assert databaseStore.isAlive();
        assert databaseStore.isEmpty();
        wrapped.put("1", "1");
        assert !databaseStore.isEmpty();
        assert databaseStore.size() == 1;
        assert wrapped.get("1").get().equals("1");
    }

    @Test
    public void testAsMap() {
        Map<String, String> map = wrapped.asMap();
        map.put("1", "1");
        assert wrapped.get("1").get().equals("1");
        assert wrapped.keySet().contains("1");
        map.put("2,", "2");
        assert !wrapped.isEmpty();
        assert wrapped.size() == 2;
        wrapped.keySet().remove("1");
        assert map.size() == 1;
    }

    @Test
    public void testPutAll() {
        Map<byte[], byte[]> rows = new ByteArrayMap<>();
        rows.put("1".getBytes(), "1".getBytes());
        rows.put("2".getBytes(), "2".getBytes());
        databaseStore.putAll(rows);
        assert databaseStore.size() == 2;
        rows = new ByteArrayMap<>();
        rows.put("1".getBytes(), databaseStore.getTrap());
        rows.put("2".getBytes(), databaseStore.getTrap());
        databaseStore.putAll(rows);
        assert databaseStore.isEmpty();
    }
}

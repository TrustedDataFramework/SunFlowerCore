package org.tdf.common.store;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.tdf.common.serialize.Codecs;

import java.util.Map;

@RunWith(JUnit4.class)
public class LevelDBTests {

    protected LevelDb databaseStore;

    protected Store<String, String> wrapped;

    @Before
    public void before(){
        databaseStore = new LevelDb("local/tmp", "leveldb");
        databaseStore.init(DBSettings.DEFAULT);
        wrapped = new StoreWrapper<>(databaseStore, Codecs.STRING, Codecs.STRING);
    }

    @After
    public void after(){
        databaseStore.clear();
        databaseStore.close();
    }

    @Test
    public void test(){
        assert databaseStore.isAlive();
        assert databaseStore.isEmpty();
        wrapped.put("1", "1");
        assert !databaseStore.isEmpty();
        assert databaseStore.size() == 1;
        assert wrapped.get("1").get().equals("1");
    }

    @Test
    public void testAsMap(){
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
}

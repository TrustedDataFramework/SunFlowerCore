package org.tdf.sunflower;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.tdf.common.store.DatabaseStore;
import org.tdf.common.store.RocksDb;
import org.tdf.sunflower.db.DatabaseStoreFactory;

@RunWith(JUnit4.class)
public class DbSourceTest {
    private static final DatabaseConfig PROPERTIES;
    private static DatabaseStoreFactory FACTORY;

    static {
        PROPERTIES = new DatabaseConfig();
        PROPERTIES.setName("rocksdb");
        PROPERTIES.setDirectory("local");
        PROPERTIES.setMaxOpenFiles(512);
        try {
            FACTORY = new DatabaseStoreFactory(PROPERTIES);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    @Test
    public void StoreTest() {
        DatabaseStore batchAbleStore = FACTORY.create("new");
        batchAbleStore.clear();
        batchAbleStore.put("lala111".getBytes(), "222".getBytes());
        assert batchAbleStore.get("lala111".getBytes()).isPresent();
        assert Arrays.equals(batchAbleStore.get("lala111".getBytes()).get(), "222".getBytes());
        batchAbleStore.remove("lala111".getBytes());
        assert !batchAbleStore.get("lala111".getBytes()).isPresent();

        batchAbleStore.remove("111".getBytes());
//        batchAbleStore.get("111".getBytes()).ifPresent(s->System.out.println(new String(s)));

        String s = "test";
        Map<byte[], byte[]> rows = new HashMap<>();
        for (int x = 0; x < 10; x++) {
            rows.put((s + x).getBytes(), (s + x).getBytes());
        }

        batchAbleStore.putAll(rows.entrySet());
        for (int x = 0; x < 10; x++) {
            int finalX = x;
            assert batchAbleStore.stream().anyMatch(y -> Arrays.equals(y.getKey(), (s + finalX).getBytes()));
        }
        for(byte[] k: rows.keySet()){
            assert batchAbleStore.get(k).isPresent();
            assert Arrays.equals(batchAbleStore.get(k).get(), k);
            if(batchAbleStore instanceof RocksDb){
//                assert batchAbleStore.prefixLookup(Arrays.copyOfRane(k, 0, k.length - 1)).isPresent();
            }
            assert !batchAbleStore.get(Arrays.copyOfRange(k, 0, k.length - 1)).isPresent();
        }

//        assert !batchAbleStore.prefixLookup("lala111".getBytes()).isPresent();
//        System.out.println(new String(batchAbleStore.prefixLookup("test".getBytes())));
    }
}

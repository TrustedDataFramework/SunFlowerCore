package org.tdf.sunflower;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.tdf.store.DatabaseStore;
import org.tdf.sunflower.db.DatabaseStoreFactory;
import org.tdf.sunflower.db.RocksDb;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RunWith(JUnit4.class)
public class DbSourceTest {
    private static final SourceDbProperties PROPERTIES;
    private static DatabaseStoreFactory FACTORY;

    static {
        PROPERTIES = new SourceDbProperties();
        PROPERTIES.setProperty("name", "rocksdb");
        PROPERTIES.setProperty("directory", "local");
        PROPERTIES.setProperty("max-open-files", "512");
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

        batchAbleStore.putAll(rows);
        for (int x = 0; x < 10; x++) {
            int finalX = x;
            assert batchAbleStore.keySet().stream().anyMatch(y -> Arrays.equals(y, (s + finalX).getBytes()));
        }
        for(byte[] k: rows.keySet()){
            assert batchAbleStore.get(k).isPresent();
            assert Arrays.equals(batchAbleStore.get(k).get(), k);
            if(batchAbleStore instanceof RocksDb){
//                assert batchAbleStore.prefixLookup(Arrays.copyOfRange(k, 0, k.length - 1)).isPresent();
            }
            assert !batchAbleStore.get(Arrays.copyOfRange(k, 0, k.length - 1)).isPresent();
        }

//        assert !batchAbleStore.prefixLookup("lala111".getBytes()).isPresent();
//        System.out.println(new String(batchAbleStore.prefixLookup("test".getBytes())));
    }
}

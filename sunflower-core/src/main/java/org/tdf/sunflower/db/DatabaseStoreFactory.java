package org.tdf.sunflower.db;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tdf.common.store.*;
import org.tdf.sunflower.DatabaseConfig;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class DatabaseStoreFactory {
    private DatabaseConfig config;
    private static final List<DatabaseStore> STORES_LIST = new ArrayList<>();

    public DatabaseStoreFactory(DatabaseConfig config) throws Exception{

        if(config.getName() == null) config.setName("");
        this.config = config;
    }

    public DatabaseStore create(String name){
        DatabaseStore store;

        switch (config.getName().trim().toLowerCase()) {
            case "leveldb":
                store = new LevelDb(config.getDirectory(), name);
                break;
            case "rocksdb":
                store = new RocksDb(config.getDirectory(), name);
                break;
            case "memory":
                store = new MemoryDatabaseStore();
                break;
            default:
                store = new RocksDb(config.getDirectory(), name);
                log.warn("Data source is not supported, default is rocksdb");
        }

        store.init(DBSettings.newInstance()
                .withMaxOpenFiles(config.getMaxOpenFiles())
                .withMaxThreads(Math.max(1, Runtime.getRuntime().availableProcessors() / 2)));
        STORES_LIST.add(store);
        if(config.isReset()){
            store.clear();
        }
        return store;
    }

    public void cleanup(){
        log.info("closing database stores...");
        STORES_LIST.forEach(DatabaseStore::close);
    }
}

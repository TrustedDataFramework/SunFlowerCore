package org.tdf.sunflower.db;

import lombok.extern.slf4j.Slf4j;
import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.impl.Iq80DBFactory;
import org.springframework.stereotype.Component;
import org.tdf.common.store.DBSettings;
import org.tdf.common.store.DatabaseStore;
import org.tdf.common.store.LevelDb;
import org.tdf.common.store.MemoryDatabaseStore;
import org.tdf.sunflower.DatabaseConfig;
import org.tdf.sunflower.facade.DatabaseStoreFactory;

import java.util.ArrayList;
import java.util.List;

@Slf4j(topic = "db")
@Component
public class DatabaseStoreFactoryImpl implements DatabaseStoreFactory {
    private static final List<DatabaseStore> STORES_LIST = new ArrayList<>();
    private final DatabaseConfig config;

    public DatabaseStoreFactoryImpl(DatabaseConfig config) {

        if (config.getName() == null) config.setName("");
        this.config = config;
    }


    public String getDirectory() {
        return config.getDirectory();
    }

    public DatabaseStore create(String name) {
        DatabaseStore store;

        switch (config.getName().trim().toLowerCase()) {
            case "leveldb-jni":
            case "leveldb":
                store = new LevelDb(JniDBFactory.factory, config.getDirectory(), name);
                break;
            case "leveldb-iq80":
                store = new LevelDb(Iq80DBFactory.factory, config.getDirectory(), name);
                break;
            case "memory":
                store = new MemoryDatabaseStore();
                break;
            default:
                store = new LevelDb(JniDBFactory.factory, config.getDirectory(), name);
                log.warn("Data source is not supported, default is leveldb");
        }

        store.init(DBSettings.newInstance()
                .withMaxOpenFiles(config.getMaxOpenFiles())
                .withMaxThreads(Math.max(1, Runtime.getRuntime().availableProcessors() / 2)));
        STORES_LIST.add(store);
        if (config.isReset()) {
            store.clear();
        }
        return store;
    }

    public void cleanup() {
        log.info("closing database stores...");
        STORES_LIST.forEach(DatabaseStore::close);
    }

    @Override
    public String getName() {
        return config.getName();
    }
}

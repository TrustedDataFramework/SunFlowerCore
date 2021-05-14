package org.tdf.sunflower.db;

import lombok.extern.slf4j.Slf4j;
import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.impl.Iq80DBFactory;
import org.springframework.stereotype.Component;
import org.tdf.common.store.*;
import org.tdf.sunflower.DatabaseConfig;
import org.tdf.sunflower.facade.DatabaseStoreFactory;


import java.util.HashSet;
import java.util.Set;

@Slf4j(topic = "db")
@Component
public class DatabaseStoreFactoryImpl implements DatabaseStoreFactory {
    private final Set<String> created;
    private final DatabaseConfig config;
    private final DatabaseStore base;

    public DatabaseStoreFactoryImpl(DatabaseConfig config) {
        created = new HashSet<>();
        if (config.getName() == null) config.setName("");
        this.config = config;
        switch (config.getName().trim().toLowerCase()) {
            case "leveldb-jni":
            case "leveldb":
                base = new LevelDb(JniDBFactory.factory, config.getDirectory());
                break;
            case "leveldb-iq80":
                base = new LevelDb(Iq80DBFactory.factory, config.getDirectory());
                break;
            case "memory":
                base = new MemoryDatabaseStore();
                break;
            default:
                base = new LevelDb(JniDBFactory.factory, config.getDirectory());
                log.warn("Data source is not supported, default is leveldb");
        }

        base.init(DBSettings.newInstance()
            .withMaxOpenFiles(config.getMaxOpenFiles())
            .withMaxThreads(Math.max(1, Runtime.getRuntime().availableProcessors() / 2)));

        if (config.isReset()) {
            base.clear();
        }
    }


    public String getDirectory() {
        return config.getDirectory();
    }

    public Store<byte[], byte[]> create(char prefix) {
        if(created.contains(new String(new char[] {prefix })))
            throw new RuntimeException("this prefix had been used");
        created.add(new String(new char[] {prefix }));
        return new BasePrefixStore(base, new byte[] {(byte) prefix});
    }


    @Override
    public String getName() {
        return config.getName();
    }
}

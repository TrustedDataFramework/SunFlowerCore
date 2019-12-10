package org.tdf.sunflower.db;

import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tdf.store.DatabaseStore;
import org.tdf.store.DbSettings;
import org.tdf.sunflower.SourceDbProperties;

import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class DatabaseStoreFactory {
    private DatabaseStoreConfig config;
    private static final List<DatabaseStore> STORES_LIST = new ArrayList<>();

    public DatabaseStoreFactory(SourceDbProperties sourceDbProperties) throws Exception{
        JavaPropsMapper mapper = new JavaPropsMapper();
        DatabaseStoreConfig config = mapper.readPropertiesAs(sourceDbProperties, DatabaseStoreConfig.class);
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
            default:
                store = new RocksDb(config.getDirectory(), name);
                log.warn("Data source is not supported, default is rocksdb");
        }

        store.init(DbSettings.newInstance()
                .withMaxOpenFiles(config.getDataMaxOpenFiles())
                .withMaxThreads(Math.max(1, Runtime.getRuntime().availableProcessors() / 2)));
        STORES_LIST.add(store);
        if(config.isReset()){
            store.clear();
        }
        return store;
    }

    @PreDestroy
    public void destroy(){
        STORES_LIST.forEach(DatabaseStore::close);
    }
}

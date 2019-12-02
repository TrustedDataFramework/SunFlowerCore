package org.wisdom.consortium.db;

import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.wisdom.common.DbSettings;
import org.wisdom.consortium.SourceDbProperties;

import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class PersistentDataStoreFactory {
    private PersistentDataStoreConfig config;
    private static final List<PersistentBinaryDataStore> STORES_LIST = new ArrayList<>();

    public PersistentDataStoreFactory(SourceDbProperties sourceDbProperties) throws Exception{
        JavaPropsMapper mapper = new JavaPropsMapper();
        PersistentDataStoreConfig config = mapper.readPropertiesAs(sourceDbProperties, PersistentDataStoreConfig.class);
        if(config.getName() == null) config.setName("");
        this.config = config;
    }

    public PersistentBinaryDataStore create(String name){
        PersistentBinaryDataStore store;

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
            store.reset();
        }
        return store;
    }

    @PreDestroy
    public void destroy(){
        STORES_LIST.forEach(PersistentBinaryDataStore::close);
    }
}

package org.tdf.common.store;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.rocksdb.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiFunction;

import static java.lang.System.arraycopy;

@Slf4j(topic = "rocksdb")
public class RocksDb implements DatabaseStore {
    static {
        RocksDB.loadLibrary();
    }

    private String directory;
    private String name;
    private RocksDB db;
    private ReadOptions readOpts;
    private DBSettings dbSettings;
    private boolean alive;
    private ReadWriteLock resetDbLock = new ReentrantReadWriteLock();

    public RocksDb(String directory, String name) {
        this.directory = directory;
        this.name = name;
        log.debug("New RocksDbDataSource: " + name);
    }

    public String getName() {
        return this.name;
    }

    public void init(DBSettings dbSettings) {
        this.dbSettings = dbSettings;
        resetDbLock.writeLock().lock();
        try {
            log.debug("~> RocksDbDataSource.init(): " + name);

            if (isAlive()) return;

            if (name == null) throw new NullPointerException("no name set to the db");

            try (Options options = new Options()) {

                // most of these options are suggested by https://github.com/facebook/rocksdb/wiki/Set-Up-Options

                // general options
                options.setCreateIfMissing(true);
                options.setCompressionType(CompressionType.LZ4_COMPRESSION);
                options.setBottommostCompressionType(CompressionType.ZSTD_COMPRESSION);
                options.setLevelCompactionDynamicLevelBytes(true);
                options.setMaxOpenFiles(dbSettings.getMaxOpenFiles());
                options.setIncreaseParallelism(dbSettings.getMaxThreads());

                // key prefix for state node lookups
                options.useFixedLengthPrefixExtractor(16);

                // table options
                final BlockBasedTableConfig tableCfg;
                options.setTableFormatConfig(tableCfg = new BlockBasedTableConfig());
                tableCfg.setBlockSize(16 * 1024);
                tableCfg.setBlockCacheSize(32 * 1024 * 1024);
                tableCfg.setCacheIndexAndFilterBlocks(true);
                tableCfg.setPinL0FilterAndIndexBlocksInCache(true);
                tableCfg.setFilter(new BloomFilter(10, false));

                // read options
                readOpts = new ReadOptions();
                readOpts = readOpts.setPrefixSameAsStart(true)
                        .setVerifyChecksums(false);

                try {
                    log.debug("Opening database");
                    final Path dbPath = getPath();
                    if (!Files.isSymbolicLink(dbPath.getParent())) Files.createDirectories(dbPath.getParent());

                    log.debug("Initializing new or existing database: '{}'", name);
                    try {
                        db = RocksDB.open(options, dbPath.toString());
                    } catch (RocksDBException e) {
                        log.error(e.getMessage(), e);
                        throw new RuntimeException("Failed to initialize database", e);
                    }

                    alive = true;

                } catch (IOException ioe) {
                    log.error(ioe.getMessage(), ioe);
                    throw new RuntimeException("Failed to initialize database", ioe);
                }

                log.debug("<~ RocksDbDataSource.init(): " + name);
            }
        } finally {
            resetDbLock.writeLock().unlock();
        }
    }

    public boolean isAlive() {
        return alive;
    }

    public void close() {
        resetDbLock.writeLock().lock();
        try {
            if (!isAlive()) return;

            log.debug("Close db: {}", name);
            db.close();
            readOpts.close();

            alive = false;

        } catch (Exception e) {
            log.error("Error closing db '{}'", name, e);
        } finally {
            resetDbLock.writeLock().unlock();
        }
    }

    public void reset() {
        close();
        FileUtil.recursiveDelete(getPath().toString());
        init(dbSettings);
    }


    @Override
    public void putAll(Collection<? extends Map.Entry<? extends byte[], ? extends byte[]>> rows) {
        resetDbLock.readLock().lock();
        try {
            try {
                try (WriteBatch batch = new WriteBatch();
                     WriteOptions writeOptions = new WriteOptions()) {
                    for (Map.Entry<? extends byte[], ? extends byte[]> entry : rows) {
                        if (entry.getValue() == null || entry.getValue() == getTrap() || entry.getValue().length == 0) {
                            batch.remove(entry.getKey());
                        } else {
                            batch.put(entry.getKey(), entry.getValue());
                        }
                    }
                    db.write(writeOptions, batch);
                }
            } catch (RocksDBException e) {
                log.error("Error in batch update on db '{}'", name, e);
                throw new RuntimeException(e);
            }
        } finally {
            resetDbLock.readLock().unlock();
        }
    }

    @Override
    public Optional<byte[]> prefixLookup(@NonNull byte[] key, int prefixBytes) {
        resetDbLock.readLock().lock();
        try {

            if (log.isTraceEnabled())
                log.trace("~> RocksDbDataSource.prefixLookup(): " + name + ", key: " + Hex.encodeHexString(key));

            // RocksDB sets initial position of iterator to the first key which is greater or equal to the seek key
            // since keys in RocksDB are ordered in asc order iterator must be initiated with the lowest key
            // thus bytes with indexes greater than PREFIX_BYTES must be nullified
            byte[] prefix = new byte[key.length];
            arraycopy(key, 0, prefix, 0, prefix.length);

            byte[] ret = null;
            try (RocksIterator it = db.newIterator(readOpts)) {
                it.seek(prefix);
                if (it.isValid())
                    ret = it.value();

            } catch (Exception e) {
                log.error("Failed to seek by prefix in db '{}'", name, e);
                throw new RuntimeException(e);
            }

            if (log.isTraceEnabled())
                log.trace("<~ RocksDbDataSource.prefixLookup(): " + name + ", key: " + Hex.encodeHexString(key) + ", " + (ret == null ? "null" : ret.length));

            return Optional.ofNullable(ret);

        } finally {
            resetDbLock.readLock().unlock();
        }
    }

    @Override
    public Optional<byte[]> get(@NonNull byte[] key) {
        resetDbLock.readLock().lock();
        try {
            if (log.isTraceEnabled())
                log.trace("~> RocksDbDataSource.get(): " + name + ", key: " + Hex.encodeHexString(key));
            byte[] ret = db.get(readOpts, key);
            if (log.isTraceEnabled())
                log.trace("<~ RocksDbDataSource.get(): " + name + ", key: " + Hex.encodeHexString(key) + ", " + (ret == null ? "null" : ret.length));
            return Optional.ofNullable(ret);
        } catch (RocksDBException e) {
            log.error("Failed to get from db '{}'", name, e);
            throw new RuntimeException(e);
        } finally {
            resetDbLock.readLock().unlock();
        }
    }

    @Override
    public void put(@NonNull byte[] key, @NonNull byte[] val) {
        resetDbLock.readLock().lock();
        try {
            if (val != getTrap() && val.length != 0) {
                db.put(key, val);
            } else {
                db.delete(key);
            }
        } catch (RocksDBException e) {
            log.error("Failed to put into db '{}'", name, e);
            throw new RuntimeException(e);
        } finally {
            resetDbLock.readLock().unlock();
        }
    }

    @Override
    public boolean containsKey(@NonNull byte[] bytes) {
        resetDbLock.readLock().lock();
        try {
            return db.get(bytes) != null;
        } catch (RocksDBException e) {
            log.error("Error get key from db '{}'", name, e);
            throw new RuntimeException(e);
        } finally {
            resetDbLock.readLock().unlock();
        }
    }

    @Override
    public void putIfAbsent(@NonNull byte[] key, @NonNull byte[] val) {
        resetDbLock.readLock().lock();
        try {
            if (db.get(key) != null) return;
            if (val != getTrap() && val.length != 0) {
                db.put(key, val);
            } else {
                db.delete(key);
            }
        } catch (Exception e) {
            log.error("Error put into db '{}'", name, e);
            throw new RuntimeException(e);
        } finally {
            resetDbLock.readLock().unlock();
        }
    }

    @Override
    public void remove(@NonNull byte[] key) {
        resetDbLock.readLock().lock();
        try {
            db.delete(key);
        } catch (RocksDBException e) {
            log.error("Failed to delete from db '{}'", name, e);
            throw new RuntimeException(e);
        } finally {
            resetDbLock.readLock().unlock();
        }
    }

    @Override
    public void clear() {
        reset();
    }

    private Path getPath() {
        return Paths.get(directory, name);
    }

    private Path backupPath() {
        return Paths.get(directory, "backup", name);
    }

    @Override
    public void flush() {

    }

    @Override
    public void traverse(BiFunction<? super byte[], ? super byte[], Boolean> traverser) {
        resetDbLock.readLock().lock();
        try {
            RocksIterator iterator = db.newIterator();
            for (iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
                if (!traverser.apply(iterator.key(), iterator.value())) {
                    return;
                }
            }
        } catch (Exception e) {
            log.error("Error iterating db '{}'", name, e);
            throw new RuntimeException(e);
        } finally {
            resetDbLock.readLock().unlock();
        }
    }
}

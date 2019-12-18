package org.tdf.sunflower.db;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.rocksdb.*;
import org.tdf.common.store.DatabaseStore;
import org.tdf.common.store.DbSettings;
import org.tdf.sunflower.util.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static java.lang.System.arraycopy;

@Slf4j
public class RocksDb implements DatabaseStore {
    private String directory;
    private String name;
    private RocksDB db;
    private ReadOptions readOpts;
    private DbSettings dbSettings;
    private boolean alive;

    private ReadWriteLock resetDbLock = new ReentrantReadWriteLock();

    static {
        RocksDB.loadLibrary();
    }

    public RocksDb(String directory, String name) {
        this.directory = directory;
        this.name = name;
        log.debug("New RocksDbDataSource: " + name);
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public void init(DbSettings dbSettings) {
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
        FileUtils.recursiveDelete(getPath().toString());
        init(dbSettings);
    }

    public Set<byte[]> keySet() throws RuntimeException {
        resetDbLock.readLock().lock();
        try {
            if (log.isTraceEnabled()) log.trace("~> RocksDbDataSource.keys(): " + name);
            try (RocksIterator iterator = db.newIterator()) {
                Set<byte[]> result = new HashSet<>();
                for (iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
                    result.add(iterator.key());
                }
                if (log.isTraceEnabled()) log.trace("<~ RocksDbDataSource.keys(): " + name + ", " + result.size());
                return result;
            } catch (Exception e) {
                log.error("Error iterating db '{}'", name, e);
                throw new RuntimeException(e);
            }
        } finally {
            resetDbLock.readLock().unlock();
        }
    }

    @Override
    public void putAll(Map<byte[], byte[]> rows) {
        resetDbLock.readLock().lock();
        try {
            try {
                try (WriteBatch batch = new WriteBatch();
                     WriteOptions writeOptions = new WriteOptions()) {
                    for (Map.Entry<byte[], byte[]> entry : rows.entrySet()) {
                        if (entry.getValue() == null) {
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
    public Optional<byte[]> prefixLookup(byte[] key, int prefixBytes) {
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
                return Optional.empty();
            }

            if (log.isTraceEnabled())
                log.trace("<~ RocksDbDataSource.prefixLookup(): " + name + ", key: " + Hex.encodeHexString(key) + ", " + (ret == null ? "null" : ret.length));

            return Optional.ofNullable(ret);

        } finally {
            resetDbLock.readLock().unlock();
        }
    }

    @Override
    public Optional<byte[]> get(byte[] key) {
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
            return Optional.empty();
        } finally {
            resetDbLock.readLock().unlock();
        }
    }

    @Override
    public void put(byte[] key, byte[] val) {
        resetDbLock.readLock().lock();
        try {
            if (val != null) {
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
    public Collection<byte[]> values() {
        resetDbLock.readLock().lock();
        try {
            try (RocksIterator iterator = db.newIterator()) {
                List<byte[]> result = new ArrayList<>();
                for (iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
                    result.add(iterator.value());
                }
                return result;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } finally {
            resetDbLock.readLock().unlock();
        }
    }

    @Override
    public boolean containsKey(byte[] bytes) {
        resetDbLock.readLock().lock();
        try {
            return db.get(bytes) != null;
        } catch (RocksDBException e) {
            e.printStackTrace();
            return false;
        } finally {
            resetDbLock.readLock().unlock();
        }
    }

    @Override
    public void putIfAbsent(byte[] key, byte[] val) {
        resetDbLock.readLock().lock();
        try {
            if (db.get(key) != null) return;
            db.put(key, val);
        }
        catch(Exception e){
            e.printStackTrace();
        } finally {
            resetDbLock.readLock().unlock();
        }
    }

    @Override
    public int size() {
        resetDbLock.readLock().lock();
        int res = 0;
        try {
            try (RocksIterator iterator = db.newIterator()) {
                for (iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
                    res ++;
                }
                return res;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } finally {
            resetDbLock.readLock().unlock();
        }
    }

    @Override
    public boolean isEmpty() {
        resetDbLock.readLock().lock();
        try {
            try (RocksIterator iterator = db.newIterator()) {
                for (iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
                    return false;
                }
                return true;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } finally {
            resetDbLock.readLock().unlock();
        }
    }

    @Override
    public void remove(byte[] key) {
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
}

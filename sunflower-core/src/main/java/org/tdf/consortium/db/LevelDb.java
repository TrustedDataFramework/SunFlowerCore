package org.wisdom.consortium.db;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.iq80.leveldb.*;
import org.wisdom.common.DbSettings;
import org.wisdom.consortium.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.fusesource.leveldbjni.JniDBFactory.factory;

@Slf4j
public class LevelDb implements PersistentBinaryDataStore {
    // subdirectory
    private String name;
    // parent directory
    private String directory;

    private DB db;
    private DbSettings dbSettings;
    private boolean alive;

    private ReadWriteLock resetDbLock = new ReentrantReadWriteLock();

    public LevelDb(String directory, String name) {
        this.directory = directory;
        this.name = name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void init(DbSettings dbsettings) {
        this.dbSettings = dbsettings;
        resetDbLock.writeLock().lock();
        try {
            log.debug("~> LevelDbDataSource.init(): " + name);

            if (isAlive()) return;

            if (name == null) throw new NullPointerException("no name set to the db");

            Options options = new Options();
            options.createIfMissing(true);
            options.compressionType(CompressionType.NONE);
            options.blockSize(10 * 1024 * 1024);
            options.writeBufferSize(10 * 1024 * 1024);
            options.cacheSize(0);
            options.paranoidChecks(true);
            options.verifyChecksums(true);
            options.maxOpenFiles(dbsettings.getMaxOpenFiles());

            try {
                log.debug("Opening database");
                final Path dbPath = getPath();
                if (!Files.isSymbolicLink(dbPath.getParent())) Files.createDirectories(dbPath.getParent());

                log.debug("Initializing new or existing database: '{}'", name);
                try {
                    db = factory.open(dbPath.toFile(), options);
                } catch (IOException e) {
                    // database could be corrupted
                    // exception in std out may look:
                    // org.fusesource.leveldbjni.internal.NativeDB$DBException: Corruption: 16 missing files; e.g.: /Users/stan/ethereumj/database-test/block/000026.ldb
                    // org.fusesource.leveldbjni.internal.NativeDB$DBException: Corruption: checksum mismatch
                    if (e.getMessage().contains("Corruption:")) {
                        log.warn("Problem initializing database.", e);
                        log.info("LevelDB database must be corrupted. Trying to repair. Could take some time.");
                        factory.repair(dbPath.toFile(), options);
                        log.info("Repair finished. Opening database again.");
                        db = factory.open(dbPath.toFile(), options);
                    } else {
                        // must be db lock
                        // org.fusesource.leveldbjni.internal.NativeDB$DBException: IO error: lock /Users/stan/ethereumj/database-test/state/LOCK: Resource temporarily unavailable
                        throw e;
                    }
                }

                alive = true;
            } catch (IOException ioe) {
                log.error(ioe.getMessage(), ioe);
                throw new RuntimeException("Can't initialize database", ioe);
            }
            log.debug("<~ LevelDbDataSource.init(): " + name);
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

            try {
                log.debug("Close db: {}", name);
                db.close();

                alive = false;
            } catch (IOException e) {
                log.error("Failed to find the db file on the close: {} ", name);
            }
        } finally {
            resetDbLock.writeLock().unlock();
        }
    }

    public void destroyDB(File fileLocation) {
        resetDbLock.writeLock().lock();
        try {
            log.debug("Destroying existing database: " + fileLocation);
            Options options = new Options();
            try {
                factory.destroy(fileLocation, options);
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        } finally {
            resetDbLock.writeLock().unlock();
        }
    }

    public void reset() {
        close();
        FileUtils.recursiveDelete(getPath().toString());
        init(dbSettings);
    }

    @Override
    public boolean containsKey(byte[] bytes) {
        resetDbLock.readLock().lock();
        try {
            return db.get(bytes) != null;
        } finally {
            resetDbLock.readLock().unlock();
        }
    }

    public Set<byte[]> keySet() throws RuntimeException {
        resetDbLock.readLock().lock();
        try {
            if (log.isTraceEnabled()) log.trace("~> LevelDbDataSource.keys(): " + name);
            try (DBIterator iterator = db.iterator()) {
                Set<byte[]> result = new HashSet<>();
                for (iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
                    result.add(iterator.peekNext().getKey());
                }
                if (log.isTraceEnabled()) log.trace("<~ LevelDbDataSource.keys(): " + name + ", " + result.size());
                return result;
            } catch (IOException e) {
                log.error("Unexpected", e);
                throw new RuntimeException(e);
            }
        } finally {
            resetDbLock.readLock().unlock();
        }
    }

    @Override
    public void updateBatch(Map<byte[], byte[]> rows) {
        resetDbLock.readLock().lock();
        try {
            if (log.isTraceEnabled()) log.trace("~> LevelDbDataSource.updateBatch(): " + name + ", " + rows.size());
            try {
                updateBatchInternal(rows);
                if (log.isTraceEnabled()) log.trace("<~ LevelDbDataSource.updateBatch(): " + name + ", " + rows.size());
            } catch (Exception e) {
                log.error("Error, retrying one more time...", e);
                // try one more time
                try {
                    updateBatchInternal(rows);
                    if (log.isTraceEnabled())
                        log.trace("<~ LevelDbDataSource.updateBatch(): " + name + ", " + rows.size());
                } catch (Exception e1) {
                    log.error("Error", e);
                    throw new RuntimeException(e);
                }
            }
        } finally {
            resetDbLock.readLock().unlock();
        }
    }

    @Override
    public Optional<byte[]> prefixLookup(byte[] key) {
        throw new RuntimeException("LevelDbDataSource.prefixLookup() is not supported");
    }

    private void updateBatchInternal(Map<byte[], byte[]> rows) throws IOException {
        try (WriteBatch batch = db.createWriteBatch()) {
            for (Map.Entry<byte[], byte[]> entry : rows.entrySet()) {
                if (entry.getValue() == null) {
                    batch.delete(entry.getKey());
                } else {
                    batch.put(entry.getKey(), entry.getValue());
                }
            }
            db.write(batch);
        }
    }

    @Override
    public Optional<byte[]> get(byte[] key) {
        resetDbLock.readLock().lock();
        try {
            if (log.isTraceEnabled())
                log.trace("~> LevelDbDataSource.get(): " + name + ", key: " + Hex.encodeHexString(key));
            try {
                byte[] ret = db.get(key);
                if (log.isTraceEnabled())
                    log.trace("<~ LevelDbDataSource.get(): " + name + ", key: " + Hex.encodeHexString(key) + ", " + (ret == null ? "null" : ret.length));
                return Optional.ofNullable(ret);
            } catch (DBException e) {
                log.warn("Exception. Retrying again...", e);
                byte[] ret = db.get(key);
                if (log.isTraceEnabled())
                    log.trace("<~ LevelDbDataSource.get(): " + name + ", key: " + Hex.encodeHexString(key) + ", " + (ret == null ? "null" : ret.length));
                return Optional.ofNullable(ret);
            }
        } finally {
            resetDbLock.readLock().unlock();
        }
    }

    @Override
    public void put(byte[] key, byte[] value) {
        resetDbLock.readLock().lock();
        try {
            if (log.isTraceEnabled())
                log.trace("~> LevelDbDataSource.put(): " + name + ", key: " + Hex.encodeHexString(key) + ", " + (value == null ? "null" : value.length));
            db.put(key, value);
            if (log.isTraceEnabled())
                log.trace("<~ LevelDbDataSource.put(): " + name + ", key: " + Hex.encodeHexString(key) + ", " + (value == null ? "null" : value.length));
        } finally {
            resetDbLock.readLock().unlock();
        }
    }

    @Override
    public Collection<byte[]> values() {
        resetDbLock.readLock().lock();
        try {
            DBIterator iterator = db.iterator();
            List<byte[]> result = new ArrayList<>();
            for (iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
                result.add(iterator.peekNext().getValue());
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            resetDbLock.readLock().unlock();
        }
    }

    @Override
    public void putIfAbsent(byte[] key, byte[] value) {
        resetDbLock.readLock().lock();
        try {
            if (db.get(key) != null) {
                return;
            }
            db.put(key, value);
        } finally {
            resetDbLock.readLock().unlock();
        }
    }

    @Override
    public void remove(byte[] key) {
        resetDbLock.readLock().lock();
        try {
            if (log.isTraceEnabled())
                log.trace("~> LevelDbDataSource.delete(): " + name + ", key: " + Hex.encodeHexString(key));
            db.delete(key);
            if (log.isTraceEnabled())
                log.trace("<~ LevelDbDataSource.delete(): " + name + ", key: " + Hex.encodeHexString(key));
        } finally {
            resetDbLock.readLock().unlock();
        }
    }

    @Override
    public int size() {
        resetDbLock.readLock().lock();
        int res = 0;
        try {
            DBIterator iterator = db.iterator();
            for (iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
                res++;
            }
            return res;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        } finally {
            resetDbLock.readLock().unlock();
        }
    }

    @Override
    public boolean isEmpty() {
        resetDbLock.readLock().lock();
        try {
            DBIterator iterator = db.iterator();
            for (iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
                return false;
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return true;
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
}

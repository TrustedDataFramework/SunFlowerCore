package org.tdf.common.store;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.iq80.leveldb.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiFunction;


@Slf4j
public class LevelDb implements DatabaseStore {
    // subdirectory
    private String name;
    // parent directory
    private String directory;

    private DB db;
    private DBSettings dbSettings;
    private boolean alive;
    private final DBFactory factory;

    private ReadWriteLock resetDbLock = new ReentrantReadWriteLock();

    public LevelDb(DBFactory factory, String directory, String name) {
        this.factory = factory;
        this.directory = directory;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void init(DBSettings dbsettings) {
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

    public void destroyDB() {
        resetDbLock.writeLock().lock();
        try {
            Options options = new Options();
            try {
                factory.destroy(getPath().toFile(), options);
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
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
    public boolean containsKey(@NonNull byte[] bytes) {
        resetDbLock.readLock().lock();
        try {
            return db.get(bytes) != null;
        } finally {
            resetDbLock.readLock().unlock();
        }
    }

    @Override
    public void putAll(Collection<? extends Map.Entry<? extends byte[], ? extends byte[]>> rows) {
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

    private void updateBatchInternal(Collection<? extends Map.Entry<? extends byte[], ? extends byte[]>> rows) throws IOException {
        try (WriteBatch batch = db.createWriteBatch()) {
            for (Map.Entry<? extends byte[], ? extends byte[]> entry : rows) {
                if (entry.getValue() == null || isTrap(entry.getValue())) {
                    batch.delete(entry.getKey());
                } else {
                    batch.put(entry.getKey(), entry.getValue());
                }
            }
            db.write(batch);
        }
    }

    @Override
    public Optional<byte[]> get(@NonNull byte[] key) {
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
    public void put(@NonNull byte[] key, @NonNull byte[] val) {
        resetDbLock.readLock().lock();
        try {
            if (log.isTraceEnabled())
                log.trace("~> LevelDbDataSource.put(): " + name + ", key: " + Hex.encodeHexString(key));
            if (isTrap(val)) {
                db.delete(key);
            } else {
                db.put(key, val);
            }
            if (log.isTraceEnabled())
                log.trace("<~ LevelDbDataSource.put(): " + name + ", key: " + Hex.encodeHexString(key));
        } finally {
            resetDbLock.readLock().unlock();
        }
    }

    @Override
    public void putIfAbsent(@NonNull byte[] key, @NonNull byte[] val) {
        resetDbLock.readLock().lock();
        try {
            if (db.get(key) != null) {
                return;
            }
            if (val != getTrap() && val.length != 0) {
                db.put(key, val);
            } else {
                db.delete(key);
            }
        } finally {
            resetDbLock.readLock().unlock();
        }
    }

    @Override
    public void remove(@NonNull byte[] key) {
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
    public void clear() {
        reset();
    }

    private Path getPath() {
        return Paths.get(directory, name);
    }

    @Override
    public void flush() {
    }

    @Override
    public void traverse(BiFunction<? super byte[], ? super byte[], Boolean> traverser) {
        resetDbLock.readLock().lock();
        try {
            if (log.isTraceEnabled()) log.trace("~> LevelDbDataSource.keys(): " + name);
            try (DBIterator iterator = db.iterator()) {
                for (iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
                    Map.Entry<byte[], byte[]> entry = iterator.peekNext();
                    if (!traverser.apply(entry.getKey(), entry.getValue())) {
                        return;
                    }
                }
            } catch (IOException e) {
                log.error("Unexpected", e);
                throw new RuntimeException(e);
            }
        } finally {
            resetDbLock.readLock().unlock();
        }
    }
}

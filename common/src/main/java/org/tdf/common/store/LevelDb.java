package org.tdf.common.store;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.iq80.leveldb.*;
import org.tdf.common.util.HexBytes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


@Slf4j(topic = "leveldb")
public class LevelDb implements DatabaseStore {
    private final DBFactory factory;
    // parent directory
    private String directory;
    private DB db;
    private DBSettings dbSettings;
    private boolean alive;
    private ReadWriteLock resetDbLock = new ReentrantReadWriteLock();

    public LevelDb(DBFactory factory, String directory) {
        this.factory = factory;
        this.directory = directory;
    }


    public void init(DBSettings dbsettings) {
        this.dbSettings = dbsettings;
        resetDbLock.writeLock().lock();
        try {
            log.debug("~> LevelDbDataSource.init(): " + directory);

            if (isAlive()) return;

            if (directory == null) throw new NullPointerException("no directory set to the db");

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

                log.debug("Initializing new or existing database: '{}'", directory);
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
            log.debug("<~ LevelDbDataSource.init(): " + directory);
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
                log.debug("Close db: {}", directory);
                db.close();

                alive = false;
            } catch (IOException e) {
                log.error("Failed to find the db file on the close: {} ", directory);
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
    public void putAll(@NonNull Collection<? extends Map.Entry<? extends byte[], ? extends byte[]>> rows) {
        resetDbLock.readLock().lock();
        try {
            if (log.isTraceEnabled())
                log.trace("~> LevelDbDataSource.updateBatch(): " + directory + ", " + rows.size());
            try {
                updateBatchInternal(rows);
                if (log.isTraceEnabled())
                    log.trace("<~ LevelDbDataSource.updateBatch(): " + directory + ", " + rows.size());
            } catch (Exception e) {
                log.error("Error, retrying one more time...", e);
                // try one more time
                try {
                    updateBatchInternal(rows);
                    if (log.isTraceEnabled())
                        log.trace("<~ LevelDbDataSource.updateBatch(): " + directory + ", " + rows.size());
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
                if (entry.getValue() == null || entry.getValue().length == 0) {
                    batch.delete(Objects.requireNonNull(entry.getKey()));
                } else {
                    batch.put(Objects.requireNonNull(entry.getKey()), entry.getValue());
                }
            }
            db.write(batch);
        }
    }

    @Override
    public byte[] get(@NonNull byte[] key) {
        resetDbLock.readLock().lock();
        try {
            if (log.isTraceEnabled())
                log.trace("~> LevelDbDataSource.get(): " + directory + ", key: " + HexBytes.encode(key));
            try {
                byte[] ret = db.get(key);
                if (log.isTraceEnabled())
                    log.trace("<~ LevelDbDataSource.get(): " + directory + ", key: " + HexBytes.encode(key) + ", " + (ret == null ? "null" : ret.length));
                return ret == null ? new byte[0] : ret;
            } catch (DBException e) {
                log.warn("Exception. Retrying again...", e);
                byte[] ret = db.get(key);
                if (log.isTraceEnabled())
                    log.trace("<~ LevelDbDataSource.get(): " + directory + ", key: " + HexBytes.encode(key) + ", " + (ret == null ? "null" : ret.length));
                return ret == null ? new byte[0] : ret;
            }
        } finally {
            resetDbLock.readLock().unlock();
        }
    }

    @Override
    public void set(@NonNull byte[] key, @NonNull byte[] val) {
        resetDbLock.readLock().lock();
        try {
            if (log.isTraceEnabled())
                log.trace("~> LevelDbDataSource.put(): " + directory + ", key: " + HexBytes.encode(key));
            if (val.length == 0) {
                db.delete(key);
            } else {
                db.put(key, val);
            }
            if (log.isTraceEnabled())
                log.trace("<~ LevelDbDataSource.put(): " + directory + ", key: " + HexBytes.encode(key));
        } finally {
            resetDbLock.readLock().unlock();
        }
    }

    @Override
    public void remove(@NonNull byte[] key) {
        resetDbLock.readLock().lock();
        try {
            if (log.isTraceEnabled())
                log.trace("~> LevelDbDataSource.delete(): " + directory + ", key: " + HexBytes.encode(key));
            db.delete(key);
            if (log.isTraceEnabled())
                log.trace("<~ LevelDbDataSource.delete(): " + directory + ", key: " + HexBytes.encode(key));
        } finally {
            resetDbLock.readLock().unlock();
        }
    }

    @Override
    public void clear() {
        reset();
    }

    private Path getPath() {
        return Paths.get(directory);
    }

    @Override
    public void flush() {
    }

}

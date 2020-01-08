package org.tdf.common.store;

import org.tdf.common.util.HexBytes;

import java.util.Optional;

/**
 * Interface represents DB source which is normally the final Source in the chain
 */
public interface DatabaseStore extends BatchStore<byte[], byte[]> {
    /**
     * Initializes DB (open table, connection, etc)
     *
     * @param settings DB settings
     */
    void init(DBSettings settings);

    @Override
    default byte[] getTrap() {
        return HexBytes.EMPTY_BYTES;
    }

    @Override
    default boolean isTrap(byte[] bytes) {
        return bytes == getTrap() || bytes.length == 0;
    }

    /**
     * @return true if DB connection is alive
     */
    boolean isAlive();

    /**
     * Closes the DB table/connection
     */
    void close();

    /**
     * If supported, retrieves a value using a key prefix.
     * Prefix extraction is meant to be done on the implementing side.<br>
     *
     * @param key         a key for the lookup
     * @param prefixBytes prefix length in bytes
     * @return first value picked by prefix lookup over DB or null if there is no match
     * @throws UnsupportedOperationException if operation is not supported
     */
    default Optional<byte[]> prefixLookup(byte[] key, int prefixBytes) {
        throw new UnsupportedOperationException();
    }


    /**
     * Closes database, destroys its data and finally runs init()
     */
    @Override
    void clear();
}

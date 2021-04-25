package org.tdf.common.store;

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


    /**
     * @return true if DB connection is alive
     */
    boolean isAlive();

    /**
     * Closes the DB table/connection
     */
    void close();

    /**
     * Closes database, destroys its data and finally runs init()
     */
    void clear();
}

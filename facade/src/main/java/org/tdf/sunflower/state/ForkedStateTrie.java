package org.tdf.sunflower.state;

import org.tdf.sunflower.types.TransactionResult;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.types.Transaction;

import java.util.Optional;

public interface ForkedStateTrie<ID, S> {
    TransactionResult update(Header header, Transaction tx);
    S remove(ID key);
    Optional<S> get(ID id);
    byte[] commit();
    void flush();
    void put(ID key, S value);
    byte[] call(HexBytes address, HexBytes args);
}

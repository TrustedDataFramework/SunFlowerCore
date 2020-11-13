package org.tdf.sunflower.state;

import org.tdf.common.types.Parameters;
import org.tdf.rlp.RLPList;
import org.tdf.sunflower.types.TransactionResult;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.types.Transaction;

import java.util.Optional;

public interface ForkedStateTrie {
    TransactionResult update(Header header, Transaction tx);
    RLPList call(HexBytes address, String method, Parameters parameters);
    byte[] getCurrentRoot();
}

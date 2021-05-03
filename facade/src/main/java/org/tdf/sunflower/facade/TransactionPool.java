package org.tdf.sunflower.facade;

import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.types.*;

import java.util.*;

public interface TransactionPool {

    // collect transactions into transaction pool, return errors
    Map<HexBytes, String> collect(Collection<? extends Transaction> transactions);

    default Map<HexBytes, String>  collect(Transaction tx) {
        return collect(Collections.singleton(tx));
    }


    // pop at most n packable transactions
    // if limit < 0, pop all transactions
    PendingData pop(Header parentHeader);
}

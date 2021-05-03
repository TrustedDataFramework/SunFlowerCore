package org.tdf.sunflower.consensus.poa;

import org.tdf.rlp.RLPElement;
import org.tdf.rlp.RLPList;
import org.tdf.sunflower.types.CryptoContext;
import org.tdf.sunflower.types.Header;

public class PoaUtils {

    public static byte[] getRawHash(Header header) {
        RLPList li = RLPElement.readRLPTree(header).asRLPList();
        // remove extraData field
        li.remove(li.size() - 3);
        return CryptoContext.hash(li.getEncoded());
    }
}

package org.tdf.sunflower.facade;

import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.types.Transaction;

public interface HashPolicy {
    HexBytes getHash(Block block);
    HexBytes getHash(Transaction transaction);
    HexBytes getHash(Header header);

    HashPolicy NONE = new HashPolicy() {
        @Override
        public HexBytes getHash(Block block) {
            return new HexBytes(new byte[32]);
        }

        @Override
        public HexBytes getHash(Transaction transaction) {
            return new HexBytes(new byte[32]);
        }

        @Override
        public HexBytes getHash(Header header) {
            return new HexBytes(new byte[32]);
        }
    };
}

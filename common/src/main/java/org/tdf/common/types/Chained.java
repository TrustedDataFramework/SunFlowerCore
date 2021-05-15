package org.tdf.common.types;

import org.tdf.common.util.HexBytes;

public interface Chained extends Hashed {
    HexBytes getHashPrev();

    default boolean isParentOf(Chained another) {
        return another.isChildOf(this);
    }

    default boolean isChildOf(Chained another) {
        return isChildOf(another.getHash());
    }

    default boolean isChildOf(HexBytes hash) {
        return getHashPrev().equals(hash);
    }
}

package org.tdf.common.types;

import org.tdf.common.util.HexBytes;

public interface Chained extends Hashed {
    HexBytes getHashPrev();

    default boolean isParentOf(Chained another){
        return this.getHash().equals(another.getHashPrev());
    }

    default boolean isChildOf(Chained another){
        return another.isParentOf(this);
    }
}

package org.tdf.common.util;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.tdf.common.types.Chained;

// if T not implements Chained, wrap it as Chained
public class ChainedWrapper<T> implements Chained {
    protected T data;
    @Getter
    @Setter(AccessLevel.PROTECTED)
    private HexBytes hashPrev;
    @Getter
    @Setter(AccessLevel.PROTECTED)
    private HexBytes hash;

    public ChainedWrapper() {
    }


    public ChainedWrapper(HexBytes hashPrev, HexBytes hash, T data) {
        this.hashPrev = hashPrev;
        this.hash = hash;
        this.data = data;
    }

    public T get() {
        return data;
    }


}

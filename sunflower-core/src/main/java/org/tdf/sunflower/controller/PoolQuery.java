package org.tdf.sunflower.controller;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import org.tdf.sunflower.types.PageSize;

@Data
public class PoolQuery implements PageSize {
    private int page;

    @Getter(AccessLevel.NONE)
    private int size;
    @Getter(AccessLevel.NONE)
    private String status;

    public int getSize() {
        return size == 0 ? Integer.MAX_VALUE : size;
    }

    public String getStatus() {
        return status == null ? "pending" : status;
    }
}

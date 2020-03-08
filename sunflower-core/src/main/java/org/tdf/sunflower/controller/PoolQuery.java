package org.tdf.sunflower.controller;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;

@Data
public class PoolQuery implements PageSize{
    private int page;

    @Getter(AccessLevel.NONE)
    private int size;

    public int getSize() {
        return size == 0 ? Integer.MAX_VALUE : size;
    }

    @Getter(AccessLevel.NONE)
    private String status;

    public String getStatus() {
        return status == null ? "pending" : status;
    }
}

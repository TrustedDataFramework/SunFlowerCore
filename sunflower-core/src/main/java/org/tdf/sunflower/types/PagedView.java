package org.tdf.sunflower.types;

import lombok.Value;

import java.util.List;

@Value
public class PagedView<T> {
    private int page;
    private int size;
    private int total;
    private List<T> records;
}

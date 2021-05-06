package org.tdf.sunflower.types;

import lombok.Value;

import java.util.Collections;
import java.util.List;

@Value
public class PagedView<T> {
    int page;
    int size;
    int total;
    List<T> records;

    public static <T> PagedView<T> empty() {
        return new PagedView<>(0, 0, 0, Collections.emptyList());
    }
}

package org.tdf.common.store;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

public class NoDeleteBatchStore<K, V> extends NoDeleteStore<K, V> implements BatchStore<K, V> {
    private BatchStore<K, V> delegate;

    public NoDeleteBatchStore(BatchStore<K, V> delegate) {
        super(delegate);
        this.delegate = delegate;
    }

    @Override
    public void putAll(Collection<? extends Map.Entry<? extends K, ? extends V>> rows) {
        delegate.putAll(rows.stream()
                .filter(x -> x.getValue() != null && !isTrap(x.getValue()))
                .collect(Collectors.toList()));
    }
}

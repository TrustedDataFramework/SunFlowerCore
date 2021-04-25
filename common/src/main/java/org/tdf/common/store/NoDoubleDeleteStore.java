package org.tdf.common.store;

import lombok.AllArgsConstructor;
import lombok.NonNull;

import java.util.function.Predicate;


@AllArgsConstructor
public class NoDoubleDeleteStore<K, V> implements Store<K, V> {
    private Store<K, V> delegate;
    private Predicate<V> isNull;

    @Override
    public V get(@NonNull K k) {
        return delegate.get(k);
    }

    @Override
    public void put(@NonNull K k, @NonNull V v) {
        delegate.put(k, v);
    }


    @Override
    public void remove(@NonNull K k) {
        V v = get(k);
        if (isNull.test(v))
            throw new RuntimeException("trying to delete a non-exists key");
        delegate.remove(k);
    }

    @Override
    public void flush() {
        delegate.flush();
    }

}

package org.tdf.common.store;

import lombok.Getter;
import lombok.NonNull;

import java.util.function.Predicate;

/**
 * no delete store will store deleted key-value pair to @see deleted
 * when compact method called, clean the key-pari in @see deleted
 */
@Getter
public class NoDeleteStore<K, V> implements Store<K, V> {
    protected Store<K, V> delegate;
    protected Predicate<V> isNull;

    public NoDeleteStore(
            Store<K, V> delegate,
            Predicate<V> isNull
    ) {
        this.delegate = delegate;
        this.isNull = isNull;
    }

    @Override
    public V get(@NonNull K k) {
        return delegate.get(k);
    }

    @Override
    public void put(@NonNull K k, @NonNull V v) {
        if (isNull.test(v)) return;
        delegate.put(k, v);
    }

    @Override
    public void remove(@NonNull K k) {
    }

    // flush all deleted to underlying db
    @Override
    public void flush() {
        delegate.flush();
    }
}

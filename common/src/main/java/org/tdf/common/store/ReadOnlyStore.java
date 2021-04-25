package org.tdf.common.store;

import lombok.NonNull;
import org.tdf.common.trie.ReadOnlyTrie;

public class ReadOnlyStore<K, V> implements Store<K, V> {
    private static final String READ_ONLY_TIP = "the store is read only";

    private Store<K, V> delegate;

    private ReadOnlyStore(Store<K, V> delegate) {
        this.delegate = delegate;
    }

    public static <K, V> Store<K, V> of(Store<K, V> delegate) {
        if (delegate instanceof ReadOnlyStore || delegate instanceof ReadOnlyTrie)
            return delegate;
        return new ReadOnlyStore<>(delegate);
    }

    @Override
    public V get(@NonNull K k) {
        return delegate.get(k);
    }

    @Override
    public void put(@NonNull K k, @NonNull V v) {
        throw new UnsupportedOperationException(READ_ONLY_TIP);
    }


    @Override
    public void remove(@NonNull K k) {
        throw new UnsupportedOperationException(READ_ONLY_TIP);
    }

    @Override
    public void flush() {
        throw new UnsupportedOperationException(READ_ONLY_TIP);
    }
}

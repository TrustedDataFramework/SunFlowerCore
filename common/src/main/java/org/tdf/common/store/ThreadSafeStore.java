package org.tdf.common.store;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

public class ThreadSafeStore<K, V> implements Store<K, V> {
    private Store<K, V> delegate;

    private ReadWriteLock lock = new ReentrantReadWriteLock();

    public ThreadSafeStore(Store<K, V> delegate) {
        this.delegate = delegate;
    }

    @Override
    public Optional<V> get(K k) {
        lock.readLock().lock();
        try {
            return delegate.get(k);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void put(K k, V v) {
        lock.writeLock().lock();
        try {
            delegate.put(k, v);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void putIfAbsent(K k, V v) {
        lock.writeLock().lock();
        try {
            delegate.putIfAbsent(k, v);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void remove(K k) {
        lock.writeLock().lock();
        try {
            delegate.remove(k);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void flush() {
        lock.writeLock().lock();
        try {
            delegate.flush();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean containsKey(K k) {
        lock.readLock().lock();
        try {
            return delegate.containsKey(k);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public int size() {
        lock.readLock().lock();
        try {
            return delegate.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean isEmpty() {
        lock.readLock().lock();
        try {
            return delegate.isEmpty();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void clear() {
        lock.writeLock().lock();
        try {
            delegate.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Map<K, V> asMap() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void forEach(BiConsumer<K, V> consumer) {
        lock.readLock().lock();
        try {
            delegate.forEach(consumer);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<K> keySet() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<V> values() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Stream<Map.Entry<K, V>> stream() {
        lock.readLock().lock();
        try {
            return delegate.stream();
        } finally {
            lock.readLock().unlock();
        }
    }
}

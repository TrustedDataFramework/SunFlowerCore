package org.tdf.common.store;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ThreadSafeStore<K, V> implements Store<K, V> {
    private Store<K, V> delegate;

    private ReadWriteLock lock = new ReentrantReadWriteLock();

    public ThreadSafeStore(Store<K, V> delegate) {
        this.delegate = delegate;
    }

    @Override
    public V get(K k) {
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


}

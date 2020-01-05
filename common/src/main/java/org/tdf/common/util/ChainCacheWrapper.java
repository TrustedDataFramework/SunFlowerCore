package org.tdf.common.util;

import lombok.NonNull;
import org.tdf.common.types.Chained;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread safe wrapper
 *
 * @param <T>
 */
class ChainCacheWrapper<T extends Chained> extends ChainCache<T> {
    protected ReadWriteLock lock;

    private ChainCache<T> delegate;

    ChainCacheWrapper(ChainCache<T> delegate) {
        this.lock = new ReentrantReadWriteLock();
        this.delegate = delegate;
    }

    @Override
    public Optional<T> get(byte[] hash) {
        lock.readLock().lock();
        try {
            return delegate.get(hash);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public ChainCache<T> clone() {
        lock.readLock().lock();
        try {
            return delegate.clone();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<T> getDescendants(byte[] hash) {
        lock.readLock().lock();
        try {
            return delegate.getDescendants(hash);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void removeDescendants(byte[] hash) {
        lock.writeLock().lock();
        try {
            delegate.removeDescendants(hash);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public List<List<T>> getAllForks() {
        lock.readLock().lock();
        try {
            return delegate.getAllForks();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void remove(byte[] hash) {
        lock.writeLock().lock();
        try {
            delegate.remove(hash);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public final void remove(Collection<byte[]> nodes) {
        lock.writeLock().lock();
        try {
            delegate.remove(nodes);
        } finally {
            lock.writeLock().unlock();
        }
    }


    @Override
    public List<T> getLeaves() {
        lock.readLock().lock();
        try {
            return delegate.getLeaves();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<T> getInitials() {
        lock.readLock().lock();
        try {
            return delegate.getInitials();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void put(T node) {
        lock.writeLock().lock();
        try {
            delegate.put(node);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void put(Collection<? extends T> nodes) {
        lock.writeLock().lock();
        try {
            delegate.put(nodes);
        } finally {
            lock.writeLock().unlock();
        }
    }


    @Override
    public List<T> getAll() {
        lock.readLock().lock();
        try {
            return delegate.getAll();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<T> popLongestChain() {
        lock.writeLock().lock();
        try {
            return delegate.popLongestChain();
        } finally {
            lock.writeLock().unlock();
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
    public boolean contains(byte[] hash) {
        lock.readLock().lock();
        try {
            return delegate.contains(hash);
        } finally {
            lock.readLock().unlock();
        }
    }


    @Override
    public List<T> getAncestors(byte[] hash) {
        lock.readLock().lock();
        try {
            return delegate.getAncestors(hash);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<T> getChildren(byte[] hash) {
        lock.readLock().lock();
        try {
            return delegate.getChildren(hash);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void putIfAbsent(@NonNull T node) {
        lock.writeLock().lock();
        try {
            delegate.putIfAbsent(node);
        } finally {
            lock.writeLock().unlock();
        }
    }
}

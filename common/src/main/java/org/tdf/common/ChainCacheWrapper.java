package org.tdf.common;

import lombok.NonNull;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread safe wrapper
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
        if (lock == null) return delegate.get(hash);
        lock.readLock().lock();
        try {
            return delegate.get(hash);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public ChainCache<T> clone() {
        if (lock == null) return delegate.clone();
        lock.readLock().lock();
        try {
            return delegate.clone();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<T> getDescendants(byte[] hash) {
        if (lock == null) return delegate.getDescendants(hash);
        lock.readLock().lock();
        try {
            return delegate.getDescendants(hash);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void removeDescendants(byte[] hash) {
        if (lock == null) {
            delegate.removeDescendants(hash);
            return;
        }
        lock.writeLock().lock();
        try {
            delegate.removeDescendants(hash);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public List<List<T>> getAllForks() {
        if(lock == null) return delegate.getAllForks();
        lock.readLock().lock();
        try {
            return delegate.getAllForks();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void remove(byte[] hash) {
        if(lock == null){
            delegate.remove(hash);
            return;
        }
        lock.writeLock().lock();
        try {
            delegate.remove(hash);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public final void remove(Collection<byte[]> nodes) {
        if(lock == null){
            delegate.remove(nodes);
            return;
        }
        lock.writeLock().lock();
        try {
            delegate.remove(nodes);
        } finally {
            lock.writeLock().unlock();
        }
    }


    @Override
    public List<T> getLeaves() {
        if(lock == null) return delegate.getLeaves();
        lock.readLock().lock();
        try {
            return delegate.getLeaves();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<T> getInitials() {
        if(lock == null) return delegate.getInitials();
        lock.readLock().lock();
        try {
            return delegate.getInitials();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void put(T node) {
        if(lock == null){
            delegate.put(node);
            return;
        }
        lock.writeLock().lock();
        try {
            delegate.put(node);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void put(Collection<? extends T> nodes) {
        if(lock == null){
            delegate.put(nodes);
            return;
        }
        lock.writeLock().lock();
        try {
            delegate.put(nodes);
        } finally {
            lock.writeLock().unlock();
        }
    }


    @Override
    public List<T> getAll() {
        if(lock == null) return delegate.getAll();
        lock.readLock().lock();
        try {
            return delegate.getAll();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<T> popLongestChain() {
        if(lock == null) return delegate.popLongestChain();
        lock.writeLock().lock();
        try {
            return delegate.popLongestChain();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public int size() {
        if(lock == null) return delegate.size();
        lock.readLock().lock();
        try {
            return delegate.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean isEmpty() {
        if(lock == null) return delegate.isEmpty();
        lock.readLock().lock();
        try {
            return delegate.isEmpty();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean contains(byte[] hash) {
        if(lock == null) return delegate.contains(hash);
        lock.readLock().lock();
        try {
            return delegate.contains(hash);
        } finally {
            lock.readLock().unlock();
        }
    }


    @Override
    public List<T> getAncestors(byte[] hash) {
        if(lock == null) return delegate.getAncestors(hash);
        lock.readLock().lock();
        try {
            return delegate.getAncestors(hash);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<T> getChildren(byte[] hash) {
        if(lock == null) return delegate.getChildren(hash);
        lock.readLock().lock();
        try {
            return delegate.getChildren(hash);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void putIfAbsent(@NonNull T node) {
        if(lock == null) {
            delegate.putIfAbsent(node);
            return;
        }
        lock.writeLock().lock();
        try {
            delegate.putIfAbsent(node);
        } finally {
            lock.writeLock().unlock();
        }
    }
}

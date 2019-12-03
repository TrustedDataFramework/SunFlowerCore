package org.tdf.common;

import lombok.NonNull;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread safe wrapper
 * @param <T>
 */
public class ChainCacheWrapper<T extends Chained> extends ChainCache<T> {
    protected ReadWriteLock lock;

    public ChainCacheWrapper(int sizeLimit, Comparator<? super T> comparator) {
        super(sizeLimit, comparator);
    }

    public ChainCacheWrapper() {
    }

    public ChainCacheWrapper(T node) {
        super(node);
    }

    public ChainCacheWrapper(Collection<? extends T> nodes) {
        super(nodes);
    }

    public ChainCache<T> withLock(){
        this.lock = new ReentrantReadWriteLock();
        return this;
    }

    @Override
    public Optional<T> get(byte[] hash) {
        if (lock == null) return super.get(hash);
        lock.readLock().lock();
        try {
            return super.get(hash);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public ChainCache<T> clone() {
        if (lock == null) return super.clone();
        lock.readLock().lock();
        try {
            return super.clone();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<T> getDescendants(byte[] hash) {
        if (lock == null) return super.getDescendants(hash);
        lock.readLock().lock();
        try {
            return super.getDescendants(hash);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void removeDescendants(byte[] hash) {
        if (lock == null) {
            super.removeDescendants(hash);
            return;
        }
        lock.writeLock().lock();
        try {
            super.removeDescendants(hash);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public List<List<T>> getAllForks() {
        if(lock == null) return super.getAllForks();
        lock.readLock().lock();
        try {
            return super.getAllForks();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void remove(byte[] hash) {
        if(lock == null){
            super.remove(hash);
            return;
        }
        lock.writeLock().lock();
        try {
            super.remove(hash);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public final void remove(Collection<byte[]> nodes) {
        if(lock == null){
            super.remove(nodes);
            return;
        }
        lock.writeLock().lock();
        try {
            super.remove(nodes);
        } finally {
            lock.writeLock().unlock();
        }
    }


    @Override
    public List<T> getLeaves() {
        if(lock == null) return super.getLeaves();
        lock.readLock().lock();
        try {
            return super.getLeaves();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<T> getInitials() {
        if(lock == null) return super.getInitials();
        lock.readLock().lock();
        try {
            return super.getInitials();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void put(T node) {
        if(lock == null){
            super.put(node);
            return;
        }
        lock.writeLock().lock();
        try {
            super.put(node);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void put(Collection<? extends T> nodes) {
        if(lock == null){
            super.put(nodes);
            return;
        }
        lock.writeLock().lock();
        try {
            super.put(nodes);
        } finally {
            lock.writeLock().unlock();
        }
    }


    @Override
    public List<T> getAll() {
        if(lock == null) return super.getAll();
        lock.readLock().lock();
        try {
            return super.getAll();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<T> popLongestChain() {
        if(lock == null) return super.popLongestChain();
        lock.writeLock().lock();
        try {
            return super.popLongestChain();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public int size() {
        if(lock == null) return super.size();
        lock.readLock().lock();
        try {
            return super.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean isEmpty() {
        if(lock == null) return super.isEmpty();
        lock.readLock().lock();
        try {
            return super.isEmpty();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean contains(byte[] hash) {
        if(lock == null) return super.contains(hash);
        lock.readLock().lock();
        try {
            return super.contains(hash);
        } finally {
            lock.readLock().unlock();
        }
    }


    @Override
    public List<T> getAncestors(byte[] hash) {
        if(lock == null) return super.getAncestors(hash);
        lock.readLock().lock();
        try {
            return super.getAncestors(hash);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<T> getChildren(byte[] hash) {
        if(lock == null) return super.getChildren(hash);
        lock.readLock().lock();
        try {
            return super.getChildren(hash);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void putIfAbsent(@NonNull T node) {
        if(lock == null) {
            super.putIfAbsent(node);
            return;
        }
        lock.writeLock().lock();
        try {
            super.putIfAbsent(node);
        } finally {
            lock.writeLock().unlock();
        }
    }
}

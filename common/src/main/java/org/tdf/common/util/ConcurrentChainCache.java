package org.tdf.common.util;

import org.tdf.common.types.Chained;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class ConcurrentChainCache<T extends Chained> implements ChainCache<T> {
    private ChainCache<T> delegate;

    private ReadWriteLock lock = new ReentrantReadWriteLock();

    private ConcurrentChainCache(ChainCache<T> delegate) {
        this.delegate = delegate;
    }

    static <T extends Chained> ConcurrentChainCache<T> of(ChainCache<T> delegate) {
        if (delegate instanceof ConcurrentChainCache)
            return (ConcurrentChainCache<T>) delegate;
        return new ConcurrentChainCache<>(delegate);
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
    public List<T> getDescendants(byte[] hash) {
        lock.readLock().lock();
        try {
            return delegate.getDescendants(hash);
        } finally {
            lock.readLock().unlock();
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
    public void removeDescendants(byte[] hash) {
        lock.writeLock().lock();
        try {
            delegate.removeDescendants(hash);
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
    public boolean removeByHash(byte[] hash) {
        lock.writeLock().lock();
        try {
            return delegate.removeByHash(hash);
        } finally {
            lock.writeLock().unlock();
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
    public boolean containsHash(byte[] hash) {
        lock.readLock().lock();
        try {
            return delegate.containsHash(hash);
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
    public Comparator<? super T> comparator() {
        return delegate.comparator();
    }

    @Override
    public SortedSet<T> subSet(T fromElement, T toElement) {
        lock.readLock().lock();
        try {
            return delegate.subSet(fromElement, toElement);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public SortedSet<T> headSet(T toElement) {
        lock.readLock().lock();
        try {
            return delegate.headSet(toElement);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public SortedSet<T> tailSet(T fromElement) {
        lock.readLock().lock();
        try {
            return delegate.tailSet(fromElement);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public T first() {
        lock.readLock().lock();
        try {
            return delegate.first();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public T last() {
        lock.readLock().lock();
        try {
            return delegate.last();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Spliterator<T> spliterator() {
        lock.readLock().lock();
        try {
            return delegate.spliterator();
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
    public boolean contains(Object o) {
        lock.readLock().lock();
        try {
            return delegate.contains(o);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Iterator<T> iterator() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(delegate).iterator();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Object[] toArray() {
        lock.readLock().lock();
        try {
            return delegate.toArray();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public <T1> T1[] toArray(T1[] a) {
        lock.readLock().lock();
        try {
            return delegate.toArray(a);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean add(T t) {
        lock.writeLock().lock();
        try {
            return delegate.add(t);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean remove(Object o) {
        lock.writeLock().lock();
        try {
            return delegate.remove(o);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        lock.readLock().lock();
        try {
            return delegate.containsAll(c);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        lock.writeLock().lock();
        try {
            return delegate.addAll(c);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        lock.writeLock().lock();
        try {
            return delegate.retainAll(c);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        lock.writeLock().lock();
        try {
            return delegate.removeAll(c);
        } finally {
            lock.writeLock().unlock();
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
    public boolean removeIf(Predicate<? super T> filter) {
        lock.writeLock().lock();
        try {
            return delegate.removeIf(filter);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Stream<T> stream() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(delegate).stream();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<T> parallelStream() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        lock.writeLock().lock();
        try {
            delegate.forEach(action);
        } finally {
            lock.writeLock().unlock();
        }
    }
}

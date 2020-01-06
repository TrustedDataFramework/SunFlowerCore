package org.tdf.common.util;

import lombok.NonNull;
import org.tdf.common.types.Chained;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

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
        return delegate.get(hash);
    }

    @Override
    public ChainCache<T> clone() {
        return delegate.clone();
    }

    @Override
    public List<T> getDescendants(byte[] hash) {
        return delegate.getDescendants(hash);
    }

    @Override
    public List<List<T>> getAllForks() {
        return delegate.getAllForks();
    }

    @Override
    public void removeDescendants(byte[] hash) {
        delegate.removeDescendants(hash);
    }

    @Override
    public List<T> getLeaves() {
        return delegate.getLeaves();
    }

    @Override
    public List<T> getInitials() {
        return delegate.getInitials();
    }

    @Override
    public boolean add(@NonNull T node) {
        return delegate.add(node);
    }

    @Override
    public boolean removeByHash(byte[] hash) {
        return delegate.removeByHash(hash);
    }

    @Override
    public boolean remove(Object o) {
        return delegate.remove(o);
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public List<T> popLongestChain() {
        return delegate.popLongestChain();
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return delegate.contains(o);
    }

    @Override
    public Iterator<T> iterator() {
        return delegate.iterator();
    }

    @Override
    public Object[] toArray() {
        return delegate.toArray();
    }

    @Override
    public <T1> T1[] toArray(T1[] a) {
        return delegate.toArray(a);
    }

    @Override
    public boolean containsHash(byte[] hash) {
        return delegate.containsHash(hash);
    }

    @Override
    public List<T> getAncestors(byte[] hash) {
        return delegate.getAncestors(hash);
    }

    @Override
    public List<T> getChildren(byte[] hash) {
        return delegate.getChildren(hash);
    }

    @Override
    public Stream<T> stream() {
        return delegate.stream();
    }

    @Override
    public Spliterator<T> spliterator() {
        return delegate.spliterator();
    }

    @Override
    public Comparator<? super T> comparator() {
        return delegate.comparator();
    }

    @Override
    public SortedSet<T> subSet(T fromElement, T toElement) {
        return delegate.subSet(fromElement, toElement);
    }

    @Override
    public SortedSet<T> headSet(T toElement) {
        return delegate.headSet(toElement);
    }

    @Override
    public SortedSet<T> tailSet(T fromElement) {
        return delegate.tailSet(fromElement);
    }

    @Override
    public T first() {
        return delegate.first();
    }

    @Override
    public T last() {
        return delegate.last();
    }
}

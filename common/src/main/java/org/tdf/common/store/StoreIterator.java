package org.tdf.common.store;

import java.util.Iterator;
import java.util.function.Consumer;

public class StoreIterator<T> implements Iterator<T> {
    private Iterator<T> iterator;

    private Consumer<T> removeAction;

    private T last;

    public StoreIterator(Iterator<T> iterator, Consumer<T> removeAction) {
        this.iterator = iterator;
        this.removeAction = removeAction;
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public T next() {
        last = iterator.next();
        return last;
    }

    @Override
    public void remove() {
        if (last != null) removeAction.accept(last);
    }
}

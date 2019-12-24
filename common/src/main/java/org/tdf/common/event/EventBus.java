package org.tdf.common.event;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

public class EventBus {
    private Map<Class, List<Consumer<Object>>> listeners = new HashMap<>();
    private ReadWriteLock lock = new ReentrantReadWriteLock();

    public <T> void subscribe(Class<T> eventType, Consumer<T> listener) {
        lock.writeLock().lock();
        try{
            listeners.putIfAbsent(eventType, new ArrayList<>());
            listeners.get(eventType).add((Consumer<Object>) listener);
        }finally {
            lock.writeLock().unlock();
        }
    }

    public void publish(Object event) {
        lock.readLock().lock();
        try {
            List<Consumer<Object>> consumers = listeners.get(event.getClass());
            if (consumers == null) return;
            consumers.forEach(con -> con.accept(event));
        } finally {
            lock.readLock().unlock();
        }
    }
}

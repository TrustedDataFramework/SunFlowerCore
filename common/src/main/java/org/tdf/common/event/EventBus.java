package org.tdf.common.event;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * lock-free event bus implementation
 */
public class EventBus {
    private final Boolean listenersLock = false;
    private Map<Class<?>, List<Consumer<Object>>> listeners = new HashMap<>();

    /**
     * subscribe a listener to event
     *
     * @param eventType type of event
     * @param listener  listener which applied when some event published
     * @param <T>       generic
     */
    public <T> void subscribe(Class<T> eventType, Consumer<? super T> listener) {
        synchronized (listenersLock) {
            // copy when write, avoid concurrent modifications
            Map<Class<?>, List<Consumer<Object>>> copied = copy(listeners);
            copied.putIfAbsent(eventType, new ArrayList<>());
            copied.get(eventType).add((Consumer<Object>) listener);
            this.listeners = copied;
        }
    }

    /**
     * publish a event to listeners
     *
     * @param event the event to publish
     */
    public void publish(Object event) {
        List<Consumer<Object>> consumers = listeners.getOrDefault(event.getClass(), Collections.emptyList());

        for (Consumer<Object> consumer : consumers) {
            try {
                CompletableFuture.runAsync(() -> consumer.accept(event));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private Map<Class<?>, List<Consumer<Object>>> copy(Map<Class<?>, List<Consumer<Object>>> listeners) {
        Map<Class<?>, List<Consumer<Object>>> ret = new HashMap<>();
        listeners.forEach((k, v) -> ret.put(k, new ArrayList<>(v)));
        return ret;
    }
}

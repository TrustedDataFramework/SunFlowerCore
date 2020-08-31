package org.tdf.common.event;

import java.util.*;
import java.util.function.Consumer;

/**
 * lock-free event bus implementation
 */
public class EventBus {
    private final Map<Class, List<Consumer<?>>> onceListeners = new HashMap<>();
    private final Boolean listenersLock = false;
    private Map<Class, List<Consumer<?>>> listeners = new HashMap<>();

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
            Map<Class, List<Consumer<?>>> copied = copy(listeners);
            copied.putIfAbsent(eventType, new ArrayList<>());
            copied.get(eventType).add(listener);
            this.listeners = copied;
        }
    }

    /**
     * subscribe a listener to event
     *
     * @param eventType type of event
     * @param listener  listener which applied when some event published
     * @param <T>       generic
     */
    public synchronized <T> void subscribeOnce(Class<T> eventType, Consumer<? super T> listener) {
        // copy when write, avoid concurrent modifications
        synchronized (onceListeners) {
            // copy when write, avoid concurrent modifications
            onceListeners.putIfAbsent(eventType, new ArrayList<>());
            onceListeners.get(eventType).add(listener);
        }
    }


    /**
     * publish a event to listeners
     *
     * @param event the event to publish
     */
    public void publish(Object event) {
        List<Consumer<?>> consumers = listeners.getOrDefault(event.getClass(), Collections.emptyList());


        for (Consumer consumer : consumers) {
            try {
                consumer.accept(event);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        synchronized (onceListeners) {
            consumers = onceListeners.remove(event.getClass());
            if (consumers == null)
                consumers = Collections.emptyList();
            for (Consumer consumer : consumers) {
                try {
                    consumer.accept(event);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private Map<Class, List<Consumer<?>>> copy(Map<Class, List<Consumer<?>>> listeners) {
        Map<Class, List<Consumer<?>>> ret = new HashMap<>();
        listeners.forEach((k, v) -> ret.put(k, new ArrayList<>(v)));
        return ret;
    }
}

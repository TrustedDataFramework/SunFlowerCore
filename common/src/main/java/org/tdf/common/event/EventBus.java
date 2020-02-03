package org.tdf.common.event;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * lock-free event bus implementation
 */
public class EventBus {
    private Map<Class, List<Consumer<?>>> listeners = new HashMap<>();

    /**
     * subscribe a listener to event
     *
     * @param eventType type of event
     * @param listener  listener which applied when some event published
     * @param <T>       generic
     */
    public synchronized <T> void subscribe(Class<T> eventType, Consumer<? super T> listener) {
        // copy when write, avoid concurrent modifications
        Map<Class, List<Consumer<?>>> copied = copy(listeners);
        copied.putIfAbsent(eventType, new ArrayList<>());
        copied.get(eventType).add(listener);
        listeners = copied;
    }

    /**
     * publish a event to listeners
     *
     * @param event the event to publish
     */
    public void publish(Object event) {
        List<Consumer<?>> consumers = listeners.get(event.getClass());
        if (consumers == null) return;
        for (Consumer consumer : consumers) {
            try {
                consumer.accept(event);
            } catch (Exception ignored) {

            }
        }
    }

    private Map<Class, List<Consumer<?>>> copy(Map<Class, List<Consumer<?>>> listeners) {
        Map<Class, List<Consumer<?>>> ret = new HashMap<>();
        listeners.forEach((k, v) -> ret.put(k, new ArrayList<>(v)));
        return ret;
    }
}

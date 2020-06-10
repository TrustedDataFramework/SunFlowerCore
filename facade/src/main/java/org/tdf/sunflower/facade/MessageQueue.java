package org.tdf.sunflower.facade;

import java.util.function.Consumer;

// message queue service
public interface MessageQueue<E, M extends Message> {
    void publish(E event, Object msg);
    void subscribe(E event, Consumer<M> listener);
}

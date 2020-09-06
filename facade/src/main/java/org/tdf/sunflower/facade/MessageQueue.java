package org.tdf.sunflower.facade;

import java.util.function.Consumer;

// message queue service
// type
// type = 0 + contract address + event name + parameters
// type = 1 + transaction hash +
// topic (1) /transaction/hash
// topic (2) /event/event-name
public interface MessageQueue<E, M extends Message> {
    void publish(E event, Object msg);
    void subscribe(E event, Consumer<M> listener);
}

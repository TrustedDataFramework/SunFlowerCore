package org.tdf.sunflower.mq;

import java.util.function.Consumer;

// message queue service
public interface MessageQueue<E, M extends Message> {
    void publish(E event, Object msg);
    void subscribe(E event, Consumer<M> listener);

    MessageQueue<String, Message> NONE = new MessageQueue<String, Message>() {
        @Override
        public void publish(String event, Object msg) {

        }

        @Override
        public void subscribe(String event, Consumer<Message> listener) {

        }
    };
}

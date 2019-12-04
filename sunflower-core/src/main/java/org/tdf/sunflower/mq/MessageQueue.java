package org.tdf.sunflower.mq;

import java.util.function.Consumer;

// message queue service
public interface MessageQueue<E, M> {
    void publish(E event, M msg);
    void subscribe(E event, Consumer<M> listener);

    MessageQueue NONE = new MessageQueue() {
        @Override
        public void publish(Object event, Object msg) {

        }

        @Override
        public void subscribe(Object event, Consumer listener) {

        }
    };
}

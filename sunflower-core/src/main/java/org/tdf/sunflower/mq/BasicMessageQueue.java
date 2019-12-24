package org.tdf.sunflower.mq;

import java.util.function.Consumer;

public interface BasicMessageQueue extends MessageQueue<String, Message>{
    BasicMessageQueue NONE = new BasicMessageQueue() {
        @Override
        public void publish(String event, Object msg) {

        }

        @Override
        public void subscribe(String event, Consumer<Message> listener) {

        }
    };
}

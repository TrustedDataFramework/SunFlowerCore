package org.tdf.sunflower.mq;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOServer;
import com.fasterxml.jackson.databind.JsonNode;
import org.tdf.sunflower.MessageQueueConfig;

import java.util.function.Consumer;

public class SocketIOMessageQueue implements BasicMessageQueue{
    private MessageQueueConfig config;
    private SocketIOServer socketIOServer;

    public SocketIOMessageQueue(MessageQueueConfig config) {
        this.config = config;
        Configuration configuration = new Configuration();
        configuration.setPort(config.getPort());
        socketIOServer = new SocketIOServer(configuration);
        socketIOServer.start();
    }

    @Override
    public void publish(String event, Object msg) {
        socketIOServer.getBroadcastOperations().sendEvent(event, msg);
    }

    @Override
    public void subscribe(String event, Consumer<Message> listener) {
        socketIOServer.addEventListener(event, JsonNode.class, new SocketIODataListener(listener));
    }
}

package org.tdf.sunflower.mq;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.listener.DataListener;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;

import java.util.function.Consumer;

@AllArgsConstructor
public class SocketIODataListener implements DataListener<JsonNode> {
    private Consumer<SocketIOData> consumer;

    @Override
    public void onData(SocketIOClient client, JsonNode data, AckRequest ackSender) throws Exception {
        consumer.accept(new SocketIOData(data));
    }
}

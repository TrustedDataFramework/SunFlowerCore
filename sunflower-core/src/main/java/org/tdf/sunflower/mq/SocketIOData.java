package org.tdf.sunflower.mq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.tdf.sunflower.Start;

// socketIO message is json-like object
public class SocketIOData {
    private JsonNode node;

    public static SocketIOData of(Object o){
        return new SocketIOData(Start.MAPPER.valueToTree(o));
    }

    SocketIOData(JsonNode node) {
        this.node = node;
    }

    public <T> T getAs(Class<T> clazz){
        try {
            return Start.MAPPER.treeToValue(node, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    JsonNode getNode() {
        return node;
    }
}

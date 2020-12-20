package org.tdf.sunflower.mq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.TreeNode;
import org.tdf.sunflower.Start;
import org.tdf.sunflower.facade.Message;

// socketIO message is json-like object
public class SocketIOMessage implements Message {
    private final TreeNode node;

    SocketIOMessage(TreeNode node) {
        this.node = node;
    }

    public <T> T as(Class<T> clazz) {
        try {
            return Start.MAPPER.treeToValue(node, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    TreeNode getNode() {
        return node;
    }
}

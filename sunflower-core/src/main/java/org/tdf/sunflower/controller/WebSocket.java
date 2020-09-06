package org.tdf.sunflower.controller;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import org.tdf.rlp.RLPCodec;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@ServerEndpoint(value = "/websocket")
@Component
public class WebSocket {
    private static final Set<WebSocket> webSocketSet = new CopyOnWriteArraySet<>();
    private Session session;

    public void init(){

    }

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        webSocketSet.add(this);
    }

    @OnClose
    public void onClose() {
        webSocketSet.remove(this);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        System.out.println("发生错误");
        error.printStackTrace();
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param messages 客户端发送过来的消息
     * @param session  可选的参数
     */
    @OnMessage
    public void onMessage(byte[] messages, Session session) {

    }

    @SneakyThrows
    public static void broadcast(byte[] data){
        for (WebSocket socket : webSocketSet) {
            socket.session.getBasicRemote().sendBinary(ByteBuffer.wrap(data, 0, data.length));
        }
    }

    // broadcast transaction success
    public static void broadcastTransaction(byte[] hash, int code, byte[] data){
        broadcast(RLPCodec.encode(new Object[]{0, hash, code, data}));
    }

    public static void broadcastEvent(byte[] address, String event, byte[] parameters){
        broadcast(RLPCodec.encode(new Object[]{1, address, event, parameters}));
    }
}

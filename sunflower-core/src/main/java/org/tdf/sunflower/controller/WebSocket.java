package org.tdf.sunflower.controller;

import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import org.tdf.rlp.RLPCodec;
import org.tdf.sunflower.types.Transaction;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@ServerEndpoint(value = "/websocket")
@Component
public class WebSocket {
    private static final Set<WebSocket> webSocketSet = new CopyOnWriteArraySet<>();
    private static final Executor EXECUTOR = Executors.newCachedThreadPool();
    private Session session;
    private final Boolean lock = true;

    public void init() {

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
    public void sendBinary(byte[] binary){
        if(this.session == null)
            return;
        synchronized (this.lock){
            this.session.getBasicRemote().sendBinary(ByteBuffer.wrap(binary,0, binary.length));
        }
    }

    @SneakyThrows
    public static void broadcastAsync(byte[] data) {
        for (WebSocket socket : webSocketSet) {
            EXECUTOR.execute(() -> {
                try {
                    socket.sendBinary(data);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    public static void broadcastDrop(Transaction tx, String reason){
        broadcastTransaction(tx.getHash().getBytes(), Transaction.DROPPED, reason.getBytes(StandardCharsets.UTF_8));
    }

    // broadcast transaction success
    public static void broadcastTransaction(byte[] hash, int code, byte[] data) {
        broadcastAsync(RLPCodec.encode(new Object[]{0, hash, code, data}));
    }

    public static void broadcastEvent(byte[] address, String event, byte[] parameters) {
        broadcastAsync(RLPCodec.encode(new Object[]{1, address, event, parameters}));
    }
}

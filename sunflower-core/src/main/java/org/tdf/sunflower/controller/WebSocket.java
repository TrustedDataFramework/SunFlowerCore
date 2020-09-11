package org.tdf.sunflower.controller;

import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import org.tdf.common.util.CopyOnWriteMap;
import org.tdf.common.util.HexBytes;
import org.tdf.rlp.RLPCodec;
import org.tdf.sunflower.types.Transaction;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@ServerEndpoint(value = "/websocket/{id}")
@Component
public class WebSocket {
    private static final Map<String, WebSocket> clients = new CopyOnWriteMap<>();
    private static final Executor EXECUTOR = Executors.newCachedThreadPool();
    private Session session;
    private final Boolean lock = true;

    private Set<HexBytes> addresses;
    private Map<HexBytes, Boolean> transactions;
    private String id;

    public void init() {

    }

    @OnOpen
    public void onOpen(Session session, @PathParam("id") String id) {
        this.id = id;
        this.session = session;
        this.addresses = new CopyOnWriteArraySet<>();
        this.transactions = new ConcurrentHashMap<>();
        clients.put(id, this);
    }

    @OnClose
    public void onClose() {
        clients.remove(this.id);
    }

    @OnError
    public void onError(Session session, Throwable error) {
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
        SubscribeMessage m = RLPCodec.decode(messages, SubscribeMessage.class);
        switch (m.getCode()){
            case 0:{
                this.transactions.put(m.getData(), true);
                break;
            }
            case 1:{
                this.addresses.add(m.getData());
                break;
            }
        }
    }

    @SneakyThrows
    public void sendBinary(byte[] binary) {
        if (this.session == null)
            return;
        synchronized (this.lock) {
            this.session.getBasicRemote().sendBinary(ByteBuffer.wrap(binary, 0, binary.length));
        }
    }

    @SneakyThrows
    public static void broadcastAsync(byte[] data, int code, HexBytes key, boolean remove) {
        for (Map.Entry<String, WebSocket> entry : clients.entrySet()) {
            EXECUTOR.execute(() -> {
                WebSocket socket = entry.getValue();
                try {
                    boolean send = false;
                    switch (code){
                        case 0:{
                            send = socket.transactions.containsKey(key);
                            if(remove)
                                    socket.transactions.remove(key);
                            break;
                        }
                        case 1:{
                            send = socket.addresses.contains(key);
                            if(remove)
                                socket.addresses.remove(key);
                            break;
                        }
                    }
                    if(send)
                        socket.sendBinary(data);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    public static void broadcastDrop(Transaction tx, String reason) {
        broadcastTransaction(tx.getHash().getBytes(), Transaction.DROPPED, reason);
    }

    // broadcast transaction success
    public static void broadcastTransaction(byte[] hash, int code, Object data) {
        broadcastAsync(RLPCodec.encode(new Object[]{0, hash, code, data}), 0, HexBytes.fromBytes(hash), code == 2 || code == 3);
    }

    public static void broadcastEvent(byte[] address, String event, Object params) {
        broadcastAsync(RLPCodec.encode(new Object[]{1, address, event, params}), 1, HexBytes.fromBytes(address), false);
    }
}

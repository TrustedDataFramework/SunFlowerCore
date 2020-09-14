package org.tdf.sunflower.controller;

import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import org.tdf.common.util.CopyOnWriteMap;
import org.tdf.common.util.HexBytes;
import org.tdf.rlp.RLPCodec;
import org.tdf.rlp.RLPElement;
import org.tdf.rlp.RLPItem;
import org.tdf.rlp.RLPList;
import org.tdf.sunflower.types.*;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Predicate;

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
        WebSocketMessage msg = RLPCodec.decode(messages, WebSocketMessage.class);
        switch (msg.getCodeEnum()){
            // 事务监听
            case TRANSACTION_SUBSCRIBE:{
                this.transactions.put(HexBytes.fromBytes(msg.getBody().asBytes()), true);
                sendNull(msg.getNonce());
                break;
            }
            // 合约监听
            case EVENT_SUBSCRIBE:{
                this.addresses.add(HexBytes.fromBytes(msg.getBody().asBytes()));
                sendNull(msg.getNonce());
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

    public void sendNull(long nonce){
        sendBinary(RLPCodec.encode(new WebSocketMessage(nonce, 0, null)));
    }

    public void sendResponse(int nonce, Object data){
        sendBinary(RLPCodec.encode(new Object[]{2, nonce, data}));
    }

    @SneakyThrows
    public static void broadcastAsync(WebSocketMessage msg, Predicate<WebSocket> when, Consumer<WebSocket> after) {
        byte[] bin = RLPCodec.encode(msg);
        for (Map.Entry<String, WebSocket> entry : clients.entrySet()) {
            EXECUTOR.execute(() -> {
                WebSocket socket = entry.getValue();
                try {
                    if (when.test(socket)) {
                        socket.sendBinary(bin);
                        after.accept(socket);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }


    public static void broadcastTransaction(Transaction tx, RLPElement body, boolean delete) {
        WebSocketMessage msg = new WebSocketMessage(
                0,
                WebSocketMessage.Code.TRANSACTION_EMIT.ordinal(),
                body
        );
        broadcastAsync(msg,
                ws -> ws.transactions.containsKey(tx.getHash()),
                ws -> { if(delete) ws.transactions.remove(tx.getHash());}
        );
    }

    public static void broadcastPendingOrConfirm(Transaction tx, Transaction.Status status) {
        RLPElement body = RLPElement.readRLPTree(new WebSocketTransactionBody(tx.getHash(), status.ordinal(), null));
        broadcastTransaction(tx, body, status == Transaction.Status.CONFIRMED);
    }

    public static void broadcastDrop(Transaction tx, String reason) {
        RLPElement body = RLPElement.readRLPTree(new WebSocketTransactionBody(tx.getHash(), Transaction.Status.DROPPED.ordinal(), reason));
        broadcastTransaction(tx, body.asRLPList(), true);
    }

    public static void broadCastIncluded(Transaction tx, long height, HexBytes blockHash, long gasUsed, RLPList returns, List<Event> events){
        WebSocketTransactionBody bd =
                new WebSocketTransactionBody(
                        tx.getHash(),
                        Transaction.Status.INCLUDED.ordinal(),
                        new Object[]{height, blockHash, gasUsed, returns, events}
                        );

        RLPElement body = RLPElement.readRLPTree(bd);
        broadcastTransaction(tx, body.asRLPList(), false);
    }

    public static void broadcastEvent(byte[] address, String event, RLPList outputs) {
        RLPElement bd = RLPElement.readRLPTree(new WebSocketEventBody(address, event, outputs)).asRLPList();
        HexBytes addr = HexBytes.fromBytes(address);
        broadcastAsync(
                new WebSocketMessage(0, WebSocketMessage.Code.EVENT_EMIT.ordinal(), bd),
                ws -> ws.addresses.contains(addr),
                ws -> {}
        );
    }
}

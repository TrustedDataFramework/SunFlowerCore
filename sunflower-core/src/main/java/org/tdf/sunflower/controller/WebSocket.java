package org.tdf.sunflower.controller;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.tdf.common.types.Parameters;
import org.tdf.common.util.HexBytes;
import org.tdf.rlp.RLPCodec;
import org.tdf.rlp.RLPElement;
import org.tdf.rlp.RLPList;
import org.tdf.sunflower.facade.SunflowerRepository;
import org.tdf.sunflower.facade.TransactionPool;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.AccountTrie;
import org.tdf.sunflower.types.*;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;

@ServerEndpoint(value = "/websocket/{id}")
@Component
@Slf4j
public class WebSocket {
    private static final Map<String, WebSocket> clients = new ConcurrentHashMap<>();
    public static ApplicationContext ctx;
    private TransactionPool transactionPool;
    private AccountTrie accountTrie;
    private SunflowerRepository repository;
    private Session session;
    private Set<HexBytes> addresses;
    private Map<HexBytes, Boolean> transactions;
    private String id;

    @SneakyThrows
    public static void broadcastAsync(WebSocketMessage msg, Predicate<WebSocket> when, Consumer<WebSocket> after) {
        byte[] bin = RLPCodec.encode(msg);
        for (Map.Entry<String, WebSocket> entry : clients.entrySet()) {
            CompletableFuture.runAsync(() -> {
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

    public static void broadcastTransaction(HexBytes txHash, RLPElement body, boolean delete) {
        WebSocketMessage msg = new WebSocketMessage(
                0,
                WebSocketMessage.Code.TRANSACTION_EMIT.ordinal(),
                body
        );
        broadcastAsync(msg,
                ws -> ws.transactions.containsKey(txHash),
                ws -> {
                    if (delete) ws.transactions.remove(txHash);
                }
        );
    }

    public static void broadcastPendingOrConfirm(HexBytes txHash, Transaction.Status status) {
        RLPElement body = RLPElement.readRLPTree(new WebSocketTransactionBody(txHash, status.ordinal(), null));
        broadcastTransaction(txHash, body, status == Transaction.Status.CONFIRMED);
    }

    public static void broadcastDrop(HexBytes hash, String reason) {
        RLPElement body = RLPElement.readRLPTree(new WebSocketTransactionBody(hash, Transaction.Status.DROPPED.ordinal(), reason));
        broadcastTransaction(hash, body.asRLPList(), true);
    }

    public static void broadCastIncluded(HexBytes txHash, long height, HexBytes blockHash, long gasUsed, RLPList returns, List<Event> events) {
        WebSocketTransactionBody bd =
                new WebSocketTransactionBody(
                        txHash,
                        Transaction.Status.INCLUDED.ordinal(),
                        new Object[]{height, blockHash, gasUsed, returns, events}
                );

        RLPElement body = RLPElement.readRLPTree(bd);
        broadcastTransaction(txHash, body.asRLPList(), false);
    }

    public static void broadcastEvent(byte[] address, String event, RLPList outputs) {
        RLPElement bd = RLPElement.readRLPTree(new WebSocketEventBody(address, event, outputs)).asRLPList();
        HexBytes addr = HexBytes.fromBytes(address);
        broadcastAsync(
                new WebSocketMessage(0, WebSocketMessage.Code.EVENT_EMIT.ordinal(), bd),
                ws -> ws.addresses.contains(addr),
                ws -> {
                }
        );
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("id") String id) {
        session.setMaxBinaryMessageBufferSize(8 * 1024 * 1024);
        this.id = id;
        this.session = session;
        this.addresses = new CopyOnWriteArraySet<>();
        this.transactions = new ConcurrentHashMap<>();
        this.transactionPool = ctx.getBean(TransactionPool.class);
        this.accountTrie = ctx.getBean(AccountTrie.class);
        this.repository = ctx.getBean(SunflowerRepository.class);
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
    public void onMessage(byte[] messages) {
        WebSocketMessage msg = RLPCodec.decode(messages, WebSocketMessage.class);
        try {
            onMessageInternal(msg);
        } catch (Exception e) {
            sendError(msg.getNonce(), e.getMessage());
        }
    }

    @SneakyThrows
    private void onMessageInternal(WebSocketMessage msg) {
        switch (msg.getCodeEnum()) {
            // 事务监听
            case TRANSACTION_SUBSCRIBE: {
                if (msg.getBody().isRLPList()) {
                    for (RLPElement element : msg.getBody().asRLPList()) {
                        this.transactions.put(HexBytes.fromBytes(element.asBytes()), true);
                    }
                } else {
                    this.transactions.put(HexBytes.fromBytes(msg.getBody().asBytes()), true);
                }
                sendNull(msg.getNonce());
                break;
            }
            // 合约监听
            case EVENT_SUBSCRIBE: {
                this.addresses.add(HexBytes.fromBytes(msg.getBody().asBytes()));
                sendNull(msg.getNonce());
                break;
            }
            // 发送事务
            case TRANSACTION_SEND: {
                log.info("transaction received");
                boolean isList = msg.getBody().get(0).asBoolean();
                Transaction[] txs = isList ? msg.getBody().get(1).as(Transaction[].class) :
                        new Transaction[]{msg.getBody().get(1).as(Transaction.class)};
                transactionPool.collect(Arrays.asList(txs));
                sendNull(msg.getNonce());
                break;
            }
            // 查看账户
            case ACCOUNT_QUERY: {
                byte[] address = msg.getBody().asBytes();
                Account a = accountTrie.get(
                        repository.getBestHeader().getStateRoot().getBytes(),
                        HexBytes.fromBytes(address)
                ).orElse(Account.emptyAccount(HexBytes.fromBytes(address)));

                sendResponse(msg.getNonce(), WebSocketMessage.Code.ACCOUNT_QUERY, a);
                break;
            }
            case CONTRACT_QUERY: {
                byte[] address = msg.getBody().get(0).asBytes();
                String method = msg.getBody().get(1).asString();
                Parameters parameters = msg.getBody().get(2)
                        .as(Parameters.class);
                byte[] root = repository.getBestHeader().getStateRoot().getBytes();
                RLPList result =  accountTrie.fork(root).call(HexBytes.fromBytes(address), method, parameters);
                sendResponse(msg.getNonce(), WebSocketMessage.Code.CONTRACT_QUERY, result);
                break;
            }
        }
    }

    @SneakyThrows
    public void sendBinary(byte[] binary) {
        if (this.session == null)
            return;
        synchronized (this) {
            this.session.getBasicRemote().sendBinary(ByteBuffer.wrap(binary, 0, binary.length));
        }
    }

    public void sendNull(long nonce) {
        sendBinary(RLPCodec.encode(new WebSocketMessage(nonce, 0, null)));
    }

    public void sendError(long nonce, String err) {
        sendBinary(RLPCodec.encode(new WebSocketMessage(nonce, WebSocketMessage.Code.ERROR.ordinal(), RLPElement.readRLPTree(err))));
    }

    public void sendResponse(long nonce, WebSocketMessage.Code code, Object data) {
        sendBinary(RLPCodec.encode(new WebSocketMessage(nonce, code.ordinal(), RLPElement.readRLPTree(data))));
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class CacheKey {
        private HexBytes contractAddress;
        private HexBytes stateRoot;
        private String method;
        private HexBytes parameters;
    }
}

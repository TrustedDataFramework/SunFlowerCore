package org.tdf.sunflower.net;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.server.WebSocketServer;
import org.tdf.sunflower.ApplicationConstants;
import org.tdf.sunflower.proto.Message;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

// net layer implemented by websocket
@Slf4j(topic = "net")
public class WebSocketNetLayer extends WebSocketServer implements NetLayer {
    private final Map<WebSocket, Channel> channels = new ConcurrentHashMap<>();
    private final MessageBuilder builder;
    private Consumer<Channel> channelHandler;

    WebSocketNetLayer(int port, MessageBuilder builder) {
        super(new InetSocketAddress(port));
        this.builder = builder;
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        ProtoChannel ch = new ProtoChannel(builder);
        ch.setOut(new WebSocketChannelOut(conn));
        channelHandler.accept(ch);
        channels.put(conn, ch);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        Channel ch = channels.get(conn);
        if (ch == null) return;
        ch.close("websocket connection closed by remote");
        channels.remove(conn);
    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer message) {
        Channel ch = channels.get(conn);
        if (ch == null) return;
        try {
            Message msg = Message.parseFrom(message);
            ch.message(msg);
        } catch (InvalidProtocolBufferException e) {
            onError(conn, e);
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {

    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        Channel ch = channels.get(conn);
        if (ch == null) return;
        ch.error(ex);
    }

    @Override
    public void onStart() {

    }

    @Override
    public void setHandler(Consumer<Channel> channelHandler) {
        this.channelHandler = channelHandler;
    }

    @Override
    public Optional<Channel> createChannel(String host, int port, ChannelListener... listeners) {
        try {
            Client client = new Client(host, port, builder);
            client.getChannel().addListeners(listeners);
            if (client.connectBlocking(1, TimeUnit.SECONDS)) {
                return Optional.of(client.getChannel());
            }
            return Optional.empty();
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    @Override
    public void close() throws IOException {
        try {
            stop((int) ApplicationConstants.MAX_SHUTDOWN_WAITING * 1000);
            log.info("websocket server closed normally");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @AllArgsConstructor
    private static class WebSocketClientChannelOut implements ProtoChannel.ChannelOut {
        private final Client client;

        @Override
        public void write(Message message) {
            client.send(message.toByteArray());
        }

        @Override
        public void close() {
            client.close();
        }
    }

    @Getter
    private static class Client extends WebSocketClient {
        private final ProtoChannel channel;

        Client(String host, int port, MessageBuilder builder) throws Exception {
            super(new URI("ws", "", host, port, "", "", ""));
            this.channel = new ProtoChannel(builder);
            this.channel.setOut(new WebSocketClientChannelOut(this));
        }

        @Override
        public void onOpen(ServerHandshake ignored) {
        }

        @Override
        public void onMessage(ByteBuffer bytes) {
            try {
                this.channel.message(Message.parseFrom(bytes));
            } catch (InvalidProtocolBufferException e) {
                channel.error(e);
            }
        }

        @Override
        public void onMessage(String message) {
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            channel.close("websocket connection closed by remote");
        }

        @Override
        public void onError(Exception ex) {
            channel.error(ex);
        }
    }

    @AllArgsConstructor
    private static class WebSocketChannelOut implements ProtoChannel.ChannelOut {
        private final WebSocket conn;

        @Override
        public void write(Message message) {
            conn.send(message.toByteArray());
        }

        @Override
        public void close() {
            try {
                conn.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
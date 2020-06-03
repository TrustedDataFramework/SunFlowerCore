package org.tdf.sunflower.net;

import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;
import lombok.Getter;
import org.tdf.sunflower.crypto.CryptoHelpers;
import org.tdf.sunflower.proto.*;
import org.tdf.sunflower.types.CryptoContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.tdf.sunflower.net.Util.getRawForSign;

public class MessageBuilder {
    @Getter
    private PeerImpl self;
    private AtomicLong nonce = new AtomicLong();

    private final PeerServerConfig config;

    public MessageBuilder(PeerImpl self, PeerServerConfig config) {
        this.self = self;
        this.config = config;
    }

    public Message buildNothing() {
        return buildMessage(Code.NOTHING, 1, Nothing.newBuilder().build().toByteArray());
    }

    public Message buildPing() {
        return buildMessage(Code.PING, 1, Ping.newBuilder().build().toByteArray());
    }

    public Message buildPong() {
        return buildMessage(Code.PONG, 1, Pong.newBuilder().build().toByteArray());
    }

    public Message buildLookup() {
        return buildMessage(Code.LOOK_UP, 1, Lookup.newBuilder().build().toByteArray());
    }


    public Message buildDisconnect(String reason) {
        return buildMessage(Code.DISCONNECT, 1, Disconnect.newBuilder().setReason(reason == null ? "" : reason).build().toByteArray());
    }

    public Message buildPeers(Collection<? extends Peer> peers) {
        return buildMessage(Code.PEERS, 1, Peers
                .newBuilder().addAllPeers(
                        peers.stream().map(Peer::encodeURI).collect(Collectors.toList())
                )
                .build().toByteArray()
        );
    }

    public List<Message> buildAnother(byte[] body, long ttl, Peer remote) {
        byte[] encryptMessage = CryptoContext.encrypt(CryptoContext.ecdh(true, self.getPrivateKey(), remote.getID().getBytes()), body);
        Message buildResult = buildMessage(Code.ANOTHER, ttl, encryptMessage);
        if (buildResult.getSerializedSize() <= config.getMaxPacketSize()) {
            return Collections.singletonList(buildResult);
        }
        byte[] serialized = buildResult.toByteArray();
        int remained = serialized.length;
        int current = 0;
        List<Message> multiParts = new ArrayList<>();
        Message.Builder builder = Message.newBuilder();
        byte[] hash = CryptoContext.hash(serialized);
        int i = 0;
        int total = serialized.length / config.getMaxPacketSize() +
                (serialized.length % config.getMaxPacketSize() == 0 ? 0 : 1);
        while (remained > 0) {
            int size = Math.min(remained, config.getMaxPacketSize());
            byte[] bodyBytes = new byte[size];
            System.arraycopy(serialized, current, bodyBytes, 0, size);
            builder.setBody(ByteString.copyFrom(bodyBytes));
            builder.setNonce(i);
            builder.setTtl(total);
            builder.setCode(Code.MULTI_PART);
            builder.setSignature(ByteString.copyFrom(hash));
            multiParts.add(builder.build());
            builder.clear();
            i ++;
            current += size;
            remained -= size;
        }
        return multiParts;
    }

    public Message buildRelay(Message message) {
        Message.Builder builder = Message.newBuilder().mergeFrom(message)
                .setCreatedAt(
                        Timestamp.newBuilder().setSeconds(System.currentTimeMillis() / 1000).build()
                )
                .setRemotePeer(self.encodeURI())
                .setNonce(nonce.incrementAndGet())
                .setTtl(message.getTtl() - 1);
        byte[] sig = CryptoContext.sign(self.getPrivateKey(), getRawForSign(builder.build()));
        return builder.setSignature(ByteString.copyFrom(sig)).build();
    }

    public Message buildMessage(Code code, long ttl, byte[] msg) {
        Message.Builder builder = Message.newBuilder()
                .setCode(code)
                .setCreatedAt(Timestamp.newBuilder().setSeconds(System.currentTimeMillis() / 1000))
                .setRemotePeer(self.encodeURI())
                .setTtl(ttl)
                .setNonce(nonce.incrementAndGet())
                .setBody(ByteString.copyFrom(msg));
        byte[] sig = CryptoContext.sign(self.getPrivateKey(), getRawForSign(builder.build()));
        return builder.setSignature(ByteString.copyFrom(sig)).build();
    }
}

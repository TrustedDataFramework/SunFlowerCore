package org.tdf.sunflower.net;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.tdf.sunflower.ApplicationConstants;
import org.tdf.sunflower.proto.EntryGrpc;
import org.tdf.sunflower.proto.Message;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j(topic = "net")
public class GRpcNetLayer extends EntryGrpc.EntryImplBase implements NetLayer {
    private final int port;
    private final MessageBuilder builder;
    private Consumer<Channel> handler;
    private Server server;

    GRpcNetLayer(int port, MessageBuilder builder) {
        this.port = port;
        this.builder = builder;
    }

    @Override
    public void start() {
        try {
            this.server = ServerBuilder.forPort(port).addService(this).build().start();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public void setHandler(Consumer<Channel> channelHandler) {
        this.handler = channelHandler;
    }

    @Override
    public Optional<Channel> createChannel(String host, int port, ChannelListener... listeners) {
        try {
            ManagedChannel ch = ManagedChannelBuilder
                    .forAddress(host, port).usePlaintext().build();
            EntryGrpc.EntryStub stub = EntryGrpc.newStub(ch);
            ProtoChannel channel = new ProtoChannel(builder);
            channel.addListeners(listeners);
            channel.setOut(new GRpcChannelOut(stub.entry(
                    new ChannelWrapper(channel)
            )));
            return Optional.of(channel);
        } catch (Throwable ignored) {
            return Optional.empty();
        }
    }

    @Override
    public StreamObserver<Message> entry(StreamObserver<Message> responseObserver) {
        ProtoChannel ch = new ProtoChannel(builder);
        ch.setOut(new GRpcChannelOut(responseObserver));
        handler.accept(ch);
        return new ChannelWrapper(ch);
    }

    @Override
    public void close() throws IOException {
        if (server.isShutdown()) return;
        try {
            server.shutdown();
            server.awaitTermination(ApplicationConstants.MAX_SHUTDOWN_WAITING, TimeUnit.SECONDS);
            log.info("gRPC server closed normally");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @AllArgsConstructor
    private static class ChannelWrapper implements StreamObserver<Message> {
        private final Channel channel;

        @Override
        public void onNext(Message value) {
            channel.message(value);
        }

        @Override
        public void onError(Throwable t) {
            channel.error(t);
        }

        @Override
        public void onCompleted() {
            channel.close("closed by remote");
        }
    }

    @AllArgsConstructor
    private static class GRpcChannelOut implements ProtoChannel.ChannelOut {
        private final StreamObserver<Message> out;

        @Override
        public void write(Message message) {
            out.onNext(message);
        }

        @Override
        public void close() {
            out.onCompleted();
        }
    }
}

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

@Slf4j
public class GRpcNetLayer extends EntryGrpc.EntryImplBase implements NetLayer {
    private Consumer<Channel> handler;

    private int port;

    private Server server;

    GRpcNetLayer(int port) {
        this.port = port;
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
    public void onChannelIncoming(Consumer<Channel> channelHandler) {
        this.handler = channelHandler;
    }

    @Override
    public Optional<Channel> createChannel(String host, int port, Channel.ChannelListener... listeners) {
        try {
            ManagedChannel ch = ManagedChannelBuilder
                    .forAddress(host, port).usePlaintext().build();
            EntryGrpc.EntryStub stub = EntryGrpc.newStub(ch);
            Channel channel = new ProtoChannel();
            channel.addListener(listeners);
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
        Channel ch = new ProtoChannel();
        ch.setOut(new GRpcChannelOut(responseObserver));
        handler.accept(ch);
        return new ChannelWrapper(ch);
    }

    @AllArgsConstructor
    private static class ChannelWrapper implements StreamObserver<Message> {
        private Channel channel;

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
            channel.close();
        }
    }

    @AllArgsConstructor
    private static class GRpcChannelOut implements Channel.ChannelOut {
        private StreamObserver<Message> out;

        @Override
        public void write(Message message) {
            out.onNext(message);
        }

        @Override
        public void close() {
            out.onCompleted();
        }
    }

    @Override
    public void close() throws IOException {
        if(server.isShutdown()) return;
        try {
            server.shutdown();
            server.awaitTermination(ApplicationConstants.MAX_SHUTDOWN_WAITING, TimeUnit.SECONDS);
            log.info("gRPC server closed normally");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

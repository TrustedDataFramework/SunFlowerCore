package org.tdf.sunflower.net

import io.grpc.ManagedChannelBuilder
import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.stub.StreamObserver
import org.slf4j.LoggerFactory
import org.tdf.sunflower.ApplicationConstants
import org.tdf.sunflower.net.ProtoChannel.ChannelOut
import org.tdf.sunflower.proto.EntryGrpc
import org.tdf.sunflower.proto.EntryGrpc.EntryImplBase
import org.tdf.sunflower.proto.Message
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

class GRpcNetLayer internal constructor(private val port: Int, private val builder: MessageBuilder) : EntryImplBase(),
    NetLayer {
    private var handler: Consumer<Channel>? = null
    private lateinit var server: Server

    override fun start() {
        try {
            server = ServerBuilder.forPort(port).addService(this).build().start()
        } catch (e: IOException) {
            throw RuntimeException(e.message)
        }
    }

    override fun setHandler(channelHandler: Consumer<Channel>) {
        handler = channelHandler
    }

    override fun createChannel(host: String, port: Int, vararg listeners: ChannelListener): Channel? {
        return try {
            val ch = ManagedChannelBuilder
                .forAddress(host, port).usePlaintext().build()
            val stub = EntryGrpc.newStub(ch)
            val channel = ProtoChannel(builder)
            channel.addListeners(*listeners)
            channel.setOut(
                GRpcChannelOut(
                    stub.entry(
                        ChannelWrapper(channel)
                    )
                )
            )
            channel
        } catch (ignored: Throwable) {
            null
        }
    }

    override fun entry(responseObserver: StreamObserver<Message>): StreamObserver<Message> {
        val ch = ProtoChannel(builder)
        ch.setOut(GRpcChannelOut(responseObserver))
        handler!!.accept(ch)
        return ChannelWrapper(ch)
    }

    override fun close() {
        if (server.isShutdown) return
        try {
            server.shutdown()
            server.awaitTermination(ApplicationConstants.MAX_SHUTDOWN_WAITING, TimeUnit.SECONDS)
            log.info("gRPC server closed normally")
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    private class ChannelWrapper(private val channel: Channel) : StreamObserver<Message> {
        override fun onNext(value: Message) {
            channel.message(value)
        }

        override fun onError(t: Throwable) {
            channel.error(t)
        }

        override fun onCompleted() {
            channel.close("closed by remote")
        }
    }

    private class GRpcChannelOut(private val out: StreamObserver<Message>) : ChannelOut {
        override fun write(message: Message) {
            out.onNext(message)
        }

        override fun close() {
            out.onCompleted()
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger("net")
    }
}
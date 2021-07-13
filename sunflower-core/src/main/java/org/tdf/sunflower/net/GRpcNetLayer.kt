package org.tdf.sunflower.net

import io.grpc.ManagedChannelBuilder
import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.stub.StreamObserver
import org.slf4j.LoggerFactory
import org.tdf.sunflower.ApplicationConstants
import org.tdf.sunflower.proto.EntryGrpc
import org.tdf.sunflower.proto.EntryGrpc.EntryImplBase
import org.tdf.sunflower.proto.Message
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

class GRpcChannelOut(var out: StreamObserver<Message>? = null) : ChannelOut {
    override fun write(message: Message) {
        out?.onNext(message)
    }

    override fun close() {
        out?.onCompleted()
    }
}

class GRpcNetLayer internal constructor(private val port: Int, private val builder: MessageBuilder) : EntryImplBase(),
    NetLayer {
    override var handler: Consumer<Channel> = Consumer { }
    private lateinit var server: Server

    override fun start() {
        try {
            server = ServerBuilder.forPort(port).addService(this).build().start()
        } catch (e: IOException) {
            throw RuntimeException(e.message)
        }
    }


    override fun createChannel(host: String, port: Int, vararg listeners: ChannelListener): Channel? {
        return try {
            val ch = ManagedChannelBuilder
                .forAddress(host, port).usePlaintext().build()
            val stub = EntryGrpc.newStub(ch)
            val nullOut = GRpcChannelOut()
            val channel = GrpcChannel(builder, nullOut)
            val out = stub.entry(channel)
            nullOut.out = out
            channel
        } catch (ignored: Throwable) {
            null
        }
    }

    override fun entry(responseObserver: StreamObserver<Message>): StreamObserver<Message> {
        val ch = GrpcChannel(builder, GRpcChannelOut(responseObserver))
        handler.accept(ch)
        return ch
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


    companion object {
        private val log = LoggerFactory.getLogger("net")
    }
}
package org.tdf.sunflower.console;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOServer;
import com.google.common.hash.Hashing;
import io.netty.util.internal.StringUtil;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.KeccakDigest;
import org.springframework.stereotype.Component;
import org.tdf.crypto.HashFunctions;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import java.nio.charset.StandardCharsets;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.UUID;

// Command line interface
@Component
public class Console {
    private final ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
    private final ScriptEngine nashorn = scriptEngineManager.getEngineByName("nashorn");
    // event for
    private static final String INPUT_EVENT = "console-in";
    private static final String OUTPUT_EVENT = "console-out";
    private Writer errorWriter = new Writer() {
        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            writeError(new String(Arrays.copyOfRange(cbuf, off, off + len)));
        }

        @Override
        public void flush() throws IOException {

        }

        @Override
        public void close() throws IOException {

        }
    };

    private Writer outWriter = new Writer() {
        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            writeOutPut(new String(Arrays.copyOfRange(cbuf, off, off + len)));
        }

        @Override
        public void flush() throws IOException {

        }

        @Override
        public void close() throws IOException {

        }
    };

    private SocketIOServer socketIOServer;
    private String uuid = UUID.randomUUID().toString();

    public Console(ConsoleConfig config) {
        Configuration configuration = new Configuration();
        configuration.setPort(config.getPort());
        socketIOServer = new SocketIOServer(configuration);
        // handle auth here
        socketIOServer.addConnectListener(client -> {
            // if session id is not valid, disconnect
            String token = client.getHandshakeData().getSingleUrlParam("token");
            if (StringUtil.isNullOrEmpty(token)) {
                client.disconnect();
                return;
            }
            if (!verifyToken(token)) {
                client.disconnect();
            }
        });
        nashorn.getContext().setWriter(outWriter);
        nashorn.getContext().setErrorWriter(errorWriter);

        socketIOServer.addEventListener(INPUT_EVENT, ConsoleIn.class, (client, data, ackSender) -> nashorn.eval(data.getInput()));

        socketIOServer.addDisconnectListener(socketIOClient -> {
            socketIOClient.sendEvent(OUTPUT_EVENT, new ConsoleOut(ConsoleOut.ERROR, "", "session id authorization failed"));
        });

        socketIOServer.start();
    }

    private boolean verifyToken(String token) {
        Digest digest = new KeccakDigest(256);
        return Arrays.toString(HashFunctions.hash(uuid.getBytes(StandardCharsets.UTF_8), digest)).equals(token);
    }

    private void regenerateUUID() {
        uuid = UUID.randomUUID().toString();
        writeTokenFile();
    }

    // read port, generate uuid write to token file
    private void writeTokenFile() {

    }

    private void writeOutPut(String output) {
        socketIOServer.getBroadcastOperations().sendEvent(OUTPUT_EVENT, new ConsoleOut(ConsoleOut.OK, output, ""));
    }

    private void writeError(String error) {
        socketIOServer.getBroadcastOperations().sendEvent(OUTPUT_EVENT, new ConsoleOut(ConsoleOut.ERROR, "", error));
    }
}

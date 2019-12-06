package org.tdf.sunflower.console;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOServer;
import io.netty.util.internal.StringUtil;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.KeccakDigest;
import org.springframework.stereotype.Component;
import org.tdf.common.HexBytes;
import org.tdf.crypto.HashFunctions;
import org.tdf.sunflower.Start;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import java.io.File;
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
    private ConsoleConfig consoleConfig;

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
        this.consoleConfig = config;
        if (config.isDisabled()) return;
        writeTokenFile();
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
                return;
            }
            String sessionID = client.getSessionId().toString();
            writeOutPut("connected:SessionId = " + sessionID);
        });
        nashorn.getContext().setWriter(outWriter);
        nashorn.getContext().setErrorWriter(errorWriter);
        socketIOServer.addEventListener(INPUT_EVENT, ConsoleIn.class, (client, data, ackSender) -> nashorn.eval(data.getInput()));

        socketIOServer.addDisconnectListener(socketIOClient -> {
            String sessionID = socketIOClient.getSessionId().toString();
            writeError("authorization failed , session id is " + sessionID);
        });

        socketIOServer.start();
    }

    private boolean verifyToken(String token) {
        return HexBytes.encode(HashFunctions.keccak256(uuid.getBytes(StandardCharsets.US_ASCII))).equals(token);
    }

    private void regenerateUUID() {
        uuid = UUID.randomUUID().toString();
        writeTokenFile();
    }

    // read port, generate uuid write to token file
    private void writeTokenFile() {
        TokenFile file = TokenFile.builder()
                .token(uuid)
                .port(consoleConfig.getPort()).build();

        try {
            Start.MAPPER.writeValue(new File(consoleConfig.getTokenFile()), file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeOutPut(String output) {
        socketIOServer.getBroadcastOperations().sendEvent(OUTPUT_EVENT, new ConsoleOut(ConsoleOut.OK, output, ""));
    }

    private void writeError(String error) {
        socketIOServer.getBroadcastOperations().sendEvent(OUTPUT_EVENT, new ConsoleOut(ConsoleOut.ERROR, "", error));
    }
}

package org.tdf.sunflower.console;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import io.netty.util.internal.StringUtil;
import org.springframework.stereotype.Component;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.crypto.CryptoContext;
import org.tdf.sunflower.Start;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.UUID;

// api for web console
//@Component
public class WebConsole {
    static final SecureRandom SECURE_RANDOM = new SecureRandom();
    static final int SALT_LENGTH = 8;

    // event for

    private ConsoleConfig consoleConfig;

    private SocketIOServer socketIOServer;

    private String uuid = UUID.randomUUID().toString();

    public WebConsole(ConsoleConfig config) {
        this.consoleConfig = config;
        if (config.isDisabled()) return;
        writeTokenFile();
        Configuration configuration = new Configuration();
        configuration.setPort(config.getPort());
        socketIOServer = new SocketIOServer(configuration);

        // handle auth here
        socketIOServer.addConnectListener(client -> {
            // if session id is not valid, disconnect
            if (closeUnauthorized(client)) {
                return;
            }
            sendInfo(client, "connect success");
        });


        socketIOServer.addEventListener(Commands.SHUTDOWN_COMMAND, Payload.class, (client, data, ackSender) -> {
            if (closeUnauthorized(client)) return;
            sendInfo(client, "application will shutdown soon...");
            System.exit(0);
        });

        socketIOServer.start();
    }

    private boolean verifyToken(String token) {
        return HexBytes.encode(CryptoContext.digest(uuid.getBytes(StandardCharsets.US_ASCII))).equals(token);
    }

    private boolean verifyToken(String token, String createdAt) {
        return HexBytes.encode(CryptoContext.digest(
                (uuid + createdAt).getBytes(StandardCharsets.US_ASCII)
        )).equals(token);
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

    private boolean closeUnauthorized(SocketIOClient client, Payload payload) {
        if(payload == null
                || Math.abs(Long.parseLong(payload.getCreatedAt()) - System.currentTimeMillis()) > 30 * 1000
                || !verifyToken(payload.getToken(), payload.getCreatedAt())
        ){
            sendError(client, "authentication failed, disconnect");
            client.disconnect();
            return true;
        }
        return false;
    }

    private boolean closeUnauthorized(SocketIOClient client) {
        String token = client.getHandshakeData().getSingleUrlParam("token");
        if (!StringUtil.isNullOrEmpty(token) && verifyToken(token)) {
            return false;
        }
        sendError(client, "authentication failed, disconnect");
        client.disconnect();
        return true;
    }

    private void sendInfo(SocketIOClient client, String info) {
        client.sendEvent(Events.LOG_EVENT, new Response(Response.OK, info, ""));
    }

    private void sendError(SocketIOClient client, String error) {
        client.sendEvent(Events.LOG_EVENT, new Response(Response.OK, "", error));
    }
}

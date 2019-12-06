package org.tdf.sunflower.console;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.DataListener;
import org.springframework.stereotype.Component;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.UUID;

// Command line interface
@Component
public class Console {
    private final ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
    private final ScriptEngine nashorn = scriptEngineManager.getEngineByName("nashorn");

    // event for
    private static final String INPUT_EVENT = "console-in";
    private static final String OUTPUT_EVENT = "console-out";


    private SocketIOServer socketIOServer;
    private String uuid = UUID.randomUUID().toString();

    public Console(ConsoleConfig config) {
        Configuration configuration = new Configuration();
        configuration.setPort(config.getPort());
        socketIOServer = new SocketIOServer(configuration);
        // handle auth here
        socketIOServer.addConnectListener(client -> {
            // if session id is not valid, disconnect
        });


        socketIOServer.addEventListener(INPUT_EVENT, ConsoleIn.class, new DataListener<ConsoleIn>() {
            @Override
            public void onData(SocketIOClient client, ConsoleIn data, AckRequest ackSender) throws Exception {
                nashorn.eval(data.getInput());
            }
        });

        socketIOServer.start();
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

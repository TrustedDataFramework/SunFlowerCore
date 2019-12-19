package org.tdf.sunflower.net;

import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import lombok.extern.slf4j.Slf4j;
import org.tdf.sunflower.facade.PeerServerListener;
import org.tdf.sunflower.util.FileUtils;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * debug configs
 * node0: X_ADDRESS=node://03c39e13db3f6c39fbefc2ca9a067bf6b9dde6c574908e805a9688752a9a43b3a47030bc7b031dbc85e5bb5a2a14d836c0eab79633654dd51413062e9d92cfc2@192.168.1.3:9090;X_ENABLE_DISCOVERY=true;X_NAME=websocket
 * node1: X_ADDRESS=node://027291ff72e196d831e08ffe01380524b00526a85f89be94c3e7c2fdcdb56d3b3b3bc60e2c8df7a01c5158a7b2791b9e2ebd3c3f04067c90fbe27237bac39d36@192.168.1.3:9091;X_BOOTSTRAPS=node://localhost:9090;X_ENABLE_DISCOVERY=true;X_NAME=websocket
 * node2: X_ADDRESS=node://00cc8638befa7841a6dd0273a6cd38bf44ad6434c3efe7e2f554f9ae60980ddaa8d1dcf9a06a4d8446cc7a9b52474b0ba980ea50fb95f4ae0b176bc0da7320d6@192.168.1.3:9092;X_BOOTSTRAPS=node://localhost:9090;X_ENABLE_DISCOVERY=true;X_NAME=websocket
 * node3: X_ADDRESS=node://0b21136053e38a0d4affa5c5fac7e82ac1d8efeb2b5d535a18fb609015b6b38eeebdc2d355ef478875f8d6344131f83cf23756ad59caa60c30afb46e3bed7143@192.168.1.3:9093;X_BOOTSTRAPS=node://localhost:9090;X_ENABLE_DISCOVERY=false;X_TRUSTED=node://localhost:9091;X_NAME=websocket
 */
@Slf4j
public class P2PDebug {

    public static void main(String... args) throws Exception {
        io.netty.util.internal.logging.InternalLoggerFactory.setDefaultFactory(new InternalLoggerFactory() {
            @Override
            protected InternalLogger newInstance(String name) {
                return new NopLogger();
            }
        });

        // port listening on
        PeerServerImpl server = new PeerServerImpl();
        Properties properties = new Properties();
        properties.load(FileUtils.getResource("p2p/node3.properties").getInputStream());
        System.out.println(properties);
        server.init(properties);
        server.addListeners(new PeerServerListener() {
            @Override
            public void onMessage(Context context, PeerServer server) {
                String m = new String(context.getMessage(), StandardCharsets.UTF_8).trim();
                if (m.equals("disconnect")) {
                    log.info("disconnect to remote " + context.getRemote());
                    context.disconnect();
                }
                if (m.equals("block")) {
                    context.block();
                }
                if (m.equals("keep")) {
                    context.keep();
                }
                if (m.startsWith("relay")) {
                    context.relay();
                }
                log.info("remote = " + context.getRemote() + " message = " + m);
            }

            @Override
            public void onStart(PeerServer server) {
            }

            @Override
            public void onNewPeer(Peer peer, PeerServer server) {
                log.info("connected to " + peer + " success");
            }

            @Override
            public void onDisconnect(Peer peer, PeerServer server) {
                log.info("channel to " + peer + " closed");
            }
        });
        server.start();
        PeersCache cache = server.getClient().peersCache;
        Scanner scanner = new Scanner(System.in);
        while (true) {
            String line = scanner.nextLine().trim();
            if (line.equals("peers")) {
                cache.getPeers().forEach(x -> System.out.println(x.encodeURI() + " " + x.score));
                cache.blocked.keySet().forEach(x -> System.out.println(x.encodeURI() + " " + x.score));
                continue;
            }
            if (line.equals("clear")) {
                for (int i = 0; i < 2000; i++) System.out.println();
                continue;
            }
            if (line.equals("self")) {
                System.out.println(server.getSelf());
                continue;
            }
            if (line.equals("trusted")) {
                server.getClient().peersCache.trusted.keySet().forEach(System.out::println);
                continue;
            }
            if(line.equals("lookup")){
                server.getClient().broadcast(
                        server.getClient().messageBuilder
                        .buildLookup()
                );
                continue;
            }
            if (line.startsWith("connect")) {
                String[] hostPort = line.substring("connect".length()).trim()
                        .split("\\s|:");
                server.getClient().dial(hostPort[0], Integer.parseInt(hostPort[1]),
                        server.getClient()
                                .messageBuilder
                                .buildPing());
                continue;
            }
            if (line.equals("bootstraps")) {
                server.getBootStraps().forEach(System.out::println);
                continue;
            }
            if (line.startsWith("broadcast")) {
                server.broadcast(line.substring("broadcast".length())
                        .trim().getBytes(StandardCharsets.UTF_8));
                continue;
            }
            List<String> arguments = Arrays.asList(line.split("\\s"));
            if (arguments.size() == 0) continue;
            for (Peer p : server.getPeers()) {
                if (p.getID().toString().startsWith(arguments.get(0))) {
                    server.dial(p,
                            String.join(" ", arguments.subList(1, arguments.size()))
                                    .getBytes(StandardCharsets.UTF_8)
                    );
                }
            }
        }
    }
}

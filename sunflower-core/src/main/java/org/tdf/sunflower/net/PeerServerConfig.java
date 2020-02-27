package org.tdf.sunflower.net;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.tdf.common.util.BigEndian;
import org.tdf.common.util.HexBytes;

import java.net.URI;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PeerServerConfig {
    public static final int DEFAULT_PORT = BigEndian.decodeInt32(new byte[]{0, 0, 'w', 'i'});
    public static final String DEFAULT_PROTOCOL = "node";
    public static final long DEFAULT_MAX_TTL = 8;
    public static final int DEFAULT_MAX_PEERS = 32;
    public static final String DEFAULT_NAME = "websocket";

    private String name;
    @JsonProperty(value = "max-peers")
    private int maxPeers;
    @JsonProperty(value = "max-ttl")
    private long maxTTL;
    @JsonProperty(value = "enable-discovery")
    private boolean enableDiscovery;
    @JsonProperty(value = "enable-message-log")
    private boolean enableMessageLog;
    private URI address;
    private List<URI> bootstraps;
    private List<URI> trusted;

    @JsonProperty(value = "white-list")
    private Set<HexBytes> whiteList;

    @JsonProperty(value = "blocked-list")
    private Set<HexBytes> blockedList;

    public static PeerServerConfig createDefault(){
        return PeerServerConfig.builder()
                .maxPeers(DEFAULT_MAX_PEERS)
                .maxTTL(DEFAULT_MAX_TTL).build();
    }
}

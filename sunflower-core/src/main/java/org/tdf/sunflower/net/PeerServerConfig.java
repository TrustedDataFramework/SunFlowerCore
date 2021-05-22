package org.tdf.sunflower.net;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.tdf.common.util.HexBytes;

import java.net.URI;
import java.util.List;
import java.util.Set;

public class PeerServerConfig {
    public static final int DEFAULT_PORT = 7010;
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
    private URI address;
    private List<URI> bootstraps;
    private List<URI> trusted;
    @JsonProperty(value = "white-list")
    private Set<HexBytes> whiteList;
    @JsonProperty(value = "blocked-list")
    private Set<HexBytes> blockedList;
    @JsonProperty(value = "private-key")
    private HexBytes privateKey;
    private boolean persist;
    @JsonProperty(value = "discover-rate")
    private int discoverRate;
    @JsonProperty(value = "max-packet-size")
    private int maxPacketSize;
    @JsonProperty(value = "cache-expired-after")
    private int cacheExpiredAfter;

    public PeerServerConfig(String name, int maxPeers, long maxTTL, boolean enableDiscovery, URI address, List<URI> bootstraps, List<URI> trusted, Set<HexBytes> whiteList, Set<HexBytes> blockedList, HexBytes privateKey, boolean persist, int discoverRate, int maxPacketSize, int cacheExpiredAfter) {
        this.name = name;
        this.maxPeers = maxPeers;
        this.maxTTL = maxTTL;
        this.enableDiscovery = enableDiscovery;
        this.address = address;
        this.bootstraps = bootstraps;
        this.trusted = trusted;
        this.whiteList = whiteList;
        this.blockedList = blockedList;
        this.privateKey = privateKey;
        this.persist = persist;
        this.discoverRate = discoverRate;
        this.maxPacketSize = maxPacketSize;
        this.cacheExpiredAfter = cacheExpiredAfter;
    }

    public PeerServerConfig() {
    }

    public static PeerServerConfigBuilder builder() {
        return new PeerServerConfigBuilder();
    }

    public String getName() {
        return (name == null || name.isEmpty()) ? DEFAULT_NAME : name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @JsonIgnore
    public boolean isBlocked(HexBytes id) {
        if (whiteList != null && !whiteList.isEmpty())
            return !whiteList.contains(id);

        return blockedList != null && blockedList.contains(id);
    }

    public int getMaxPeers() {
        return this.maxPeers;
    }

    public void setMaxPeers(int maxPeers) {
        this.maxPeers = maxPeers;
    }

    public long getMaxTTL() {
        return this.maxTTL;
    }

    public void setMaxTTL(long maxTTL) {
        this.maxTTL = maxTTL;
    }

    public boolean isEnableDiscovery() {
        return this.enableDiscovery;
    }

    public void setEnableDiscovery(boolean enableDiscovery) {
        this.enableDiscovery = enableDiscovery;
    }

    public URI getAddress() {
        return this.address;
    }

    public void setAddress(URI address) {
        this.address = address;
    }

    public List<URI> getBootstraps() {
        return this.bootstraps;
    }

    public void setBootstraps(List<URI> bootstraps) {
        this.bootstraps = bootstraps;
    }

    public List<URI> getTrusted() {
        return this.trusted;
    }

    public void setTrusted(List<URI> trusted) {
        this.trusted = trusted;
    }

    public Set<HexBytes> getWhiteList() {
        return this.whiteList;
    }

    public void setWhiteList(Set<HexBytes> whiteList) {
        this.whiteList = whiteList;
    }

    public Set<HexBytes> getBlockedList() {
        return this.blockedList;
    }

    public void setBlockedList(Set<HexBytes> blockedList) {
        this.blockedList = blockedList;
    }

    public HexBytes getPrivateKey() {
        return this.privateKey;
    }

    public void setPrivateKey(HexBytes privateKey) {
        this.privateKey = privateKey;
    }

    public boolean isPersist() {
        return this.persist;
    }

    public void setPersist(boolean persist) {
        this.persist = persist;
    }

    public int getDiscoverRate() {
        return this.discoverRate;
    }

    public void setDiscoverRate(int discoverRate) {
        this.discoverRate = discoverRate;
    }

    public int getMaxPacketSize() {
        return this.maxPacketSize;
    }

    public void setMaxPacketSize(int maxPacketSize) {
        this.maxPacketSize = maxPacketSize;
    }

    public int getCacheExpiredAfter() {
        return this.cacheExpiredAfter;
    }

    public void setCacheExpiredAfter(int cacheExpiredAfter) {
        this.cacheExpiredAfter = cacheExpiredAfter;
    }


    public String toString() {
        return "PeerServerConfig(name=" + this.getName() + ", maxPeers=" + this.getMaxPeers() + ", maxTTL=" + this.getMaxTTL() + ", enableDiscovery=" + this.isEnableDiscovery() + ", address=" + this.getAddress() + ", bootstraps=" + this.getBootstraps() + ", trusted=" + this.getTrusted() + ", whiteList=" + this.getWhiteList() + ", blockedList=" + this.getBlockedList() + ", privateKey=" + this.getPrivateKey() + ", persist=" + this.isPersist() + ", discoverRate=" + this.getDiscoverRate() + ", maxPacketSize=" + this.getMaxPacketSize() + ", cacheExpiredAfter=" + this.getCacheExpiredAfter() + ")";
    }

    public static class PeerServerConfigBuilder {
        private String name;
        private int maxPeers;
        private long maxTTL;
        private boolean enableDiscovery;
        private URI address;
        private List<URI> bootstraps;
        private List<URI> trusted;
        private Set<HexBytes> whiteList;
        private Set<HexBytes> blockedList;
        private HexBytes privateKey;
        private boolean persist;
        private int discoverRate;
        private int maxPacketSize;
        private int cacheExpiredAfter;

        PeerServerConfigBuilder() {
        }

        public PeerServerConfig.PeerServerConfigBuilder name(String name) {
            this.name = name;
            return this;
        }

        public PeerServerConfig.PeerServerConfigBuilder maxPeers(int maxPeers) {
            this.maxPeers = maxPeers;
            return this;
        }

        public PeerServerConfig.PeerServerConfigBuilder maxTTL(long maxTTL) {
            this.maxTTL = maxTTL;
            return this;
        }

        public PeerServerConfig.PeerServerConfigBuilder enableDiscovery(boolean enableDiscovery) {
            this.enableDiscovery = enableDiscovery;
            return this;
        }

        public PeerServerConfig.PeerServerConfigBuilder address(URI address) {
            this.address = address;
            return this;
        }

        public PeerServerConfig.PeerServerConfigBuilder bootstraps(List<URI> bootstraps) {
            this.bootstraps = bootstraps;
            return this;
        }

        public PeerServerConfig.PeerServerConfigBuilder trusted(List<URI> trusted) {
            this.trusted = trusted;
            return this;
        }

        public PeerServerConfig.PeerServerConfigBuilder whiteList(Set<HexBytes> whiteList) {
            this.whiteList = whiteList;
            return this;
        }

        public PeerServerConfig.PeerServerConfigBuilder blockedList(Set<HexBytes> blockedList) {
            this.blockedList = blockedList;
            return this;
        }

        public PeerServerConfig.PeerServerConfigBuilder privateKey(HexBytes privateKey) {
            this.privateKey = privateKey;
            return this;
        }

        public PeerServerConfig.PeerServerConfigBuilder persist(boolean persist) {
            this.persist = persist;
            return this;
        }

        public PeerServerConfig.PeerServerConfigBuilder discoverRate(int discoverRate) {
            this.discoverRate = discoverRate;
            return this;
        }

        public PeerServerConfig.PeerServerConfigBuilder maxPacketSize(int maxPacketSize) {
            this.maxPacketSize = maxPacketSize;
            return this;
        }

        public PeerServerConfig.PeerServerConfigBuilder cacheExpiredAfter(int cacheExpiredAfter) {
            this.cacheExpiredAfter = cacheExpiredAfter;
            return this;
        }

        public PeerServerConfig build() {
            return new PeerServerConfig(name, maxPeers, maxTTL, enableDiscovery, address, bootstraps, trusted, whiteList, blockedList, privateKey, persist, discoverRate, maxPacketSize, cacheExpiredAfter);
        }

        public String toString() {
            return "PeerServerConfig.PeerServerConfigBuilder(name=" + this.name + ", maxPeers=" + this.maxPeers + ", maxTTL=" + this.maxTTL + ", enableDiscovery=" + this.enableDiscovery + ", address=" + this.address + ", bootstraps=" + this.bootstraps + ", trusted=" + this.trusted + ", whiteList=" + this.whiteList + ", blockedList=" + this.blockedList + ", privateKey=" + this.privateKey + ", persist=" + this.persist + ", discoverRate=" + this.discoverRate + ", maxPacketSize=" + this.maxPacketSize + ", cacheExpiredAfter=" + this.cacheExpiredAfter + ")";
        }
    }
}

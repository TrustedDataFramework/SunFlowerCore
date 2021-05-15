package org.tdf.sunflower.net;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.SneakyThrows;
import org.tdf.common.crypto.ECKey;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.state.Address;
import org.tdf.sunflower.types.Transaction;

import java.net.URI;
import java.util.Optional;


public class PeerImpl implements Peer, Comparable<PeerImpl> {

    long score;
    private String protocol;
    private String host;
    private int port;
    private HexBytes ID;
    @JsonIgnore
    private byte[] privateKey;

    public PeerImpl(long score, String protocol, String host, int port, HexBytes ID, byte[] privateKey) {
        this.score = score;
        this.protocol = protocol;
        this.host = host;
        this.port = port;
        this.ID = ID;
        this.privateKey = privateKey;
    }

    private PeerImpl() {
    }

    public static Optional<PeerImpl> parse(String url) {
        try {
            return Optional.of(parseInternal(url));
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    // parse peer from uri like protocol://id@host:port
    // the id should be an ec public key
    @SneakyThrows
    private static PeerImpl parseInternal(String url) {
        URI u = new URI(url.trim());
        PeerImpl p = new PeerImpl();
        String scheme = u.getScheme();
        p.protocol = (scheme == null || scheme.equals("")) ? PeerServerConfig.DEFAULT_PROTOCOL : scheme;
        p.port = u.getPort();
        if (p.port <= 0) {
            p.port = PeerServerConfig.DEFAULT_PORT;
        }
        p.host = u.getHost();
        if (u.getRawUserInfo() == null || u.getRawUserInfo().isEmpty())
            throw new RuntimeException("parse peer failed: missing public key");
        p.ID = HexBytes.fromHex(u.getRawUserInfo());
        if (p.ID.size() != Transaction.ADDRESS_LENGTH) {
            throw new RuntimeException("peer " + url + " address should be " + Transaction.ADDRESS_LENGTH);
        }
        return p;
    }

    // create self as peer from input
    // if private key is missing, generate key automatically
    public static PeerImpl createSelf(URI u) {
        return createSelf(u, new ECKey().getPrivKeyBytes());
    }

    // create self as peer from input
    // if private key is missing, generate key automatically
    public static PeerImpl createSelf(URI u, byte[] privateKey) {
        if (u.getRawUserInfo() != null && !u.getRawUserInfo().isEmpty()) {
            throw new RuntimeException(u.getUserInfo() + " should be empty");
        }
        PeerImpl ret = new PeerImpl();
        String scheme = u.getScheme();
        ret.protocol = (scheme == null || scheme.equals("")) ? PeerServerConfig.DEFAULT_PROTOCOL : scheme;
        int port = u.getPort();
        ret.host = (u.getHost() == null || u.getHost().trim().equals("")) ? "localhost" : u.getHost();
        ret.port = port <= 0 ? PeerServerConfig.DEFAULT_PORT : port;
        ret.ID = Address.fromPrivate(privateKey);
        ret.privateKey = privateKey;
        return ret;
    }

    public static void main(String[] args) throws Exception {
        PeerImpl p2 = createSelf(new URI("enode://localhost"));
        System.out.println(p2);
    }

    public String toString() {
        return String.format("%s://%s@%s:%d", protocol, ID, host, port);
    }

    @Override
    public String encodeURI() {
        return toString();
    }

    public int distance(PeerImpl that) {
        int res = 0;
        byte[] bits = new byte[Transaction.ADDRESS_LENGTH];
        for (int i = 0; i < bits.length; i++) {
            bits[i] = (byte) (ID.getBytes()[i] ^ that.ID.getBytes()[i]);
        }
        for (int i = 0; i < bits.length; i++) {
            for (int j = 0; j < 7; j++) {
                res += ((1 << j) & bits[i]) >>> j;
            }
        }
        return res;
    }

    int subTree(byte[] thatID) {
        byte[] bits = new byte[Transaction.ADDRESS_LENGTH];
        byte mask = (byte) (1 << 7);
        for (int i = 0; i < bits.length; i++) {
            bits[i] = (byte) (ID.getBytes()[i] ^ thatID[i]);
        }
        for (int i = 0; i < Transaction.ADDRESS_LENGTH * 8; i++) {
            if ((bits[i / 8] & (mask >>> (i % 8))) != 0) {
                return i;
            }
        }
        return 0;
    }

    // subtree is less than PUBLIC_KEY_SIZE * 8
    int subTree(Peer that) {
        return subTree(that.getID().getBytes());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Peer)) return false;
        Peer peer = (Peer) o;
        return ID.equals(peer.getID());
    }

    @Override
    public int hashCode() {
        return ID.hashCode();
    }

    @Override
    public int compareTo(PeerImpl o) {
        return ID.compareTo(o.ID);
    }

    public long getScore() {
        return this.score;
    }

    public String getProtocol() {
        return this.protocol;
    }

    public String getHost() {
        return this.host;
    }

    public int getPort() {
        return this.port;
    }

    public HexBytes getID() {
        return this.ID;
    }

    public byte[] getPrivateKey() {
        return this.privateKey;
    }

    public void setScore(long score) {
        this.score = score;
    }

    public void setHost(String host) {
        this.host = host;
    }
}

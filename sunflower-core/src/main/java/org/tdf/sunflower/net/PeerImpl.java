package org.tdf.sunflower.net;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.exception.ApplicationException;
import org.tdf.sunflower.types.CryptoContext;

import java.net.URI;
import java.util.Optional;


@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class PeerImpl implements Peer, Comparable<PeerImpl> {

    private String protocol;

    @Setter
    private String host;
    private int port;
    private HexBytes ID;

    @JsonIgnore
    private byte[] privateKey;


    @Setter
    long score;

    public String toString() {
        return String.format("%s://%s@%s:%d", protocol, ID, host, port);
    }


    @Override
    public String encodeURI() {
        return toString();
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
            throw new ApplicationException("parse peer failed: missing public key");
        p.ID = HexBytes.fromHex(u.getRawUserInfo());
        if (p.ID.size() != CryptoContext.getPublicKeySize()) {
            throw new ApplicationException("peer " + url + " public key size should be " + CryptoContext.getPublicKeySize());
        }
        return p;
    }

    // create self as peer from input
    // if private key is missing, generate key automatically
    public static PeerImpl createSelf(URI u) {
        return createSelf(u, CryptoContext.generateSecretKey());
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
        ret.ID = HexBytes.fromBytes(CryptoContext.getPkFromSk(privateKey));
        ret.privateKey = privateKey;
        return ret;
    }


    public int distance(PeerImpl that) {
        int res = 0;
        byte[] bits = new byte[CryptoContext.getPublicKeySize()];
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
        byte[] bits = new byte[CryptoContext.getPublicKeySize()];
        byte mask = (byte) (1 << 7);
        for (int i = 0; i < bits.length; i++) {
            bits[i] = (byte) (ID.getBytes()[i] ^ thatID[i]);
        }
        for (int i = 0; i < CryptoContext.getPublicKeySize() * 8; i++) {
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

    public static void main(String[] args) throws Exception {
        PeerImpl p2 = createSelf(new URI("enode://localhost"));
        System.out.println(p2);
    }
}

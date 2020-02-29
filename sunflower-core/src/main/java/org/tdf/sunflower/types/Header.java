package org.tdf.sunflower.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.*;
import org.tdf.common.types.Chained;
import org.tdf.common.util.Constants;
import org.tdf.common.util.EpochSecondDeserializer;
import org.tdf.common.util.EpochSecondsSerializer;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.crypto.CryptoContext;
import org.tdf.rlp.RLP;
import org.tdf.rlp.RLPCodec;
import org.tdf.rlp.RLPIgnored;

import java.util.Objects;
import java.util.stream.Stream;

@Getter
@ToString
@NoArgsConstructor
public class Header implements Chained {
    /**
     * magic version number
     */
    @RLP(0)
    protected int version;
    /**
     * hash of parent block
     */
    @RLP(1)
    protected HexBytes hashPrev;
    /**
     * root hash of transaction trie
     */
    @RLP(2)
    protected HexBytes transactionsRoot;
    /**
     * root hash of state trie
     */
    @RLP(3)
    protected HexBytes stateRoot;
    /**
     * height of current header
     */
    @RLP(4)
    protected long height;
    /**
     * unix epoch when the block mined
     */
    @JsonSerialize(using = EpochSecondsSerializer.class)
    @JsonDeserialize(using = EpochSecondDeserializer.class)
    @RLP(5)
    protected long createdAt;
    /**
     * custom data
     */
    @RLP(6)
    protected HexBytes payload;
    /**
     * hash of the block
     */
    @RLPIgnored
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    protected transient HexBytes hash;


    @Builder
    public Header(
            int version, HexBytes hashPrev, HexBytes transactionsRoot,
            HexBytes stateRoot, long height, long createdAt,
            HexBytes payload
    ) {
        this.version = version;
        this.hashPrev = hashPrev;
        this.transactionsRoot = transactionsRoot;
        this.stateRoot = stateRoot;
        this.height = height;
        this.createdAt = createdAt;
        this.payload = payload;
    }

    public HexBytes getHash() {
        return getHash(false);
    }

    private HexBytes getHash(boolean forceReHash) {
        if (forceReHash || this.hash == null) {
            this.hash = HexBytes.fromBytes(
                    CryptoContext.digest(RLPCodec.encode(this))
            );
            return this.hash;
        }
        return this.hash;
    }

    public void setVersion(int version) {
        this.version = version;
        getHash(true);
    }

    public void setHashPrev(HexBytes hashPrev) {
        this.hashPrev = hashPrev;
        getHash(true);
    }

    public void setTransactionsRoot(HexBytes transactionsRoot) {
        this.transactionsRoot = transactionsRoot;
        getHash(true);
    }

    public void setStateRoot(HexBytes stateRoot) {
        this.stateRoot = stateRoot;
        getHash(true);
    }

    public void setHeight(long height) {
        this.height = height;
        getHash(true);
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
        getHash(true);
    }

    public void setPayload(HexBytes payload) {
        this.payload = payload;
        getHash(true);
    }

    @Override
    public Header clone() {
        return new Header(
                version, hashPrev, transactionsRoot, stateRoot,
                height, createdAt, payload
        );
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public int size() {
        return Constants.sizeOf(version) + Constants.sizeOf(height) +
                Constants.sizeOf(createdAt) +
                Stream.of(hashPrev, transactionsRoot, payload, hash)
                        .map(Constants::sizeOf)
                        .reduce(0, Integer::sum);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Header header = (Header) o;
        return version == header.version &&
                height == header.height &&
                createdAt == header.createdAt &&
                Objects.equals(hashPrev, header.hashPrev) &&
                Objects.equals(transactionsRoot, header.transactionsRoot) &&
                Objects.equals(stateRoot, header.stateRoot) &&
                Objects.equals(payload, header.payload);
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, hashPrev, transactionsRoot, stateRoot, height, createdAt, payload);
    }
}

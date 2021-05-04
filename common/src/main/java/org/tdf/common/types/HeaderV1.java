package org.tdf.common.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.*;
import org.tdf.common.util.*;
import org.tdf.rlp.RLP;
import org.tdf.rlp.RLPCodec;
import org.tdf.rlp.RLPIgnored;

@Getter
@ToString
@NoArgsConstructor
public class HeaderV1 implements Chained {
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
    @JsonSerialize(using = IntSerializer.class)
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
    public HeaderV1(
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
                HashUtil.sha3(RLPCodec.encode(this))
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
    public HeaderV1 clone() {
        return new HeaderV1(
            version, hashPrev, transactionsRoot, stateRoot,
            height, createdAt, payload
        );
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public int size() {
        return 0;
    }

}


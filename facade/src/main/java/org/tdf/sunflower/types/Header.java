package org.tdf.sunflower.types;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.*;
import org.tdf.common.types.Chained;
import org.tdf.common.util.*;
import org.tdf.rlp.RLPCodec;
import org.tdf.rlp.RLPIgnored;
import org.tdf.sunflower.state.Address;

import java.util.Objects;


@Getter
@ToString
@NoArgsConstructor
public class Header implements Chained {
    /**
     * hash of parent block
     */
    protected HexBytes hashPrev = ByteUtil.ZEROS_32;

    /**
     * uncles list = rlp([])
     */
    protected HexBytes unclesHash;

    /**
     * miner
     */
    protected HexBytes coinbase;

    /**
     * root hash of state trie
     */
    protected HexBytes stateRoot = HashUtil.EMPTY_TRIE_HASH_HEX;

    /**
     * root hash of transaction trie
     */
    protected HexBytes transactionsRoot = HashUtil.EMPTY_TRIE_HASH_HEX;

    /**
     * receipts root
     */
    private HexBytes receiptTrieRoot = HashUtil.EMPTY_TRIE_HASH_HEX;

    /**
     * logs bloom
     */
    private HexBytes logsBloom = Bloom.EMPTY;

    /**
     * difficulty value = EMPTY_BYTES
     */
    private HexBytes difficulty;

    /**
     * height of current header
     */
    @JsonSerialize(using = IntSerializer.class)
    protected long height = 0;

    protected HexBytes gasLimit = HexBytes.EMPTY;

    protected long gasUsed = 0;

    /**
     * unix epoch when the block mined
     */
    @JsonSerialize(using = EpochSecondsSerializer.class)
    @JsonDeserialize(using = EpochSecondDeserializer.class)
    protected long createdAt = 0;

    // <= 32 bytes
    protected HexBytes extraData;

    // = 32byte
    protected HexBytes mixHash;

    // = 8byte
    protected HexBytes nonce;


    /**
     * hash of the block
     */
    @RLPIgnored
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    protected transient HexBytes hash;

    @Builder
    public Header(
        HexBytes hashPrev, HexBytes coinbase, HexBytes stateRoot,
        HexBytes transactionsRoot, HexBytes receiptTrieRoot,
        HexBytes logsBloom, long height, HexBytes gasLimit,
        long gasUsed, long createdAt, HexBytes extraData
    ) {
        this.coinbase = coinbase == null ? Address.empty() : coinbase;
        this.receiptTrieRoot = receiptTrieRoot == null ? HashUtil.EMPTY_TRIE_HASH_HEX : receiptTrieRoot;
        this.logsBloom = logsBloom == null ? Bloom.EMPTY : logsBloom;
        this.gasLimit = gasLimit == null ? HexBytes.empty() : gasLimit;
        this.gasUsed = gasUsed;
        this.extraData = extraData == null ? HexBytes.empty() : extraData;
        this.hashPrev = hashPrev == null ? ByteUtil.ZEROS_32 : hashPrev;
        this.transactionsRoot = transactionsRoot == null ? HashUtil.EMPTY_TRIE_HASH_HEX : transactionsRoot;
        this.stateRoot = stateRoot == null ? HashUtil.EMPTY_TRIE_HASH_HEX : stateRoot;
        this.height = height;
        this.createdAt = createdAt;

        // useless for tdos
        this.unclesHash = HexBytes.fromBytes(HashUtil.EMPTY_LIST_HASH);
        this.difficulty = HexBytes.empty();
        this.mixHash = HexBytes.empty();
        this.nonce = HexBytes.empty();
    }

    public HexBytes getHash() {
        return getHash(false);
    }

    public void resetHash() {
        this.hash = null;
    }

    private HexBytes getHash(boolean forceReHash) {
        if (forceReHash || this.hash == null) {
            this.hash = HexBytes.fromBytes(
                CryptoContext.hash(RLPCodec.encode(this))
            );
            return this.hash;
        }
        return this.hash;
    }


    public void setHashPrev(HexBytes hashPrev) {
        this.hashPrev = hashPrev;
        resetHash();
    }

    public void setTransactionsRoot(HexBytes transactionsRoot) {
        this.transactionsRoot = transactionsRoot;
        resetHash();
    }

    public void setStateRoot(HexBytes stateRoot) {
        this.stateRoot = stateRoot;
        resetHash();
    }

    public void setHeight(Long height) {
        this.height = Objects.requireNonNull(height);
        resetHash();
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = Objects.requireNonNull(createdAt);
        resetHash();
    }


    public void setCoinbase(HexBytes coinbase) {
        this.coinbase = coinbase;
        resetHash();
    }

    public void setGasLimit(HexBytes gasLimit) {
        this.gasLimit = gasLimit;
        resetHash();
    }

    public void setGasUsed(Long gasUsed) {
        this.gasUsed = Objects.requireNonNull(gasUsed);
        resetHash();
    }

    public void setExtraData(HexBytes extraData) {
        this.extraData = extraData;
        resetHash();
    }

    public void setMixHash(HexBytes mixHash) {
        this.mixHash = mixHash;
        resetHash();
    }

    public void setNonce(HexBytes nonce) {
        this.nonce = nonce;
        resetHash();
    }

    public void setReceiptTrieRoot(HexBytes receiptTrieRoot) {
        this.receiptTrieRoot = receiptTrieRoot;
        resetHash();
    }

    public void setLogsBloom(HexBytes logsBloom) {
        this.logsBloom = logsBloom;
        resetHash();
    }
}

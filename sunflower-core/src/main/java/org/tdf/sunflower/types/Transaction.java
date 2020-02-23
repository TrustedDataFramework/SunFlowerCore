package org.tdf.sunflower.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.*;
import org.tdf.common.util.Constants;
import org.tdf.common.util.EpochSecondDeserializer;
import org.tdf.common.util.EpochSecondsSerializer;
import org.tdf.common.util.HexBytes;
import org.tdf.crypto.HashFunctions;
import org.tdf.rlp.RLP;
import org.tdf.rlp.RLPCodec;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
public class Transaction {
    public enum Type {
        // coinbase transaction has code 0
        COIN_BASE(0x00),
        // the amount is transferred from sender to recipient
        // if type is transfer, payload is null
        // and fee is a constant
        TRANSFER(0x01),
        // if type is contract deploy, payload is wasm binary module
        // fee = gasPrice * gasUsage
        CONTRACT_DEPLOY(0x02),
        // if type is contract call, payload = gasLimit(little endian, 8 bytes) +
        // method name length (an unsigned byte) +
        // method name(ascii string, [_a-zA-Z][_a-zA-Z0-9]*) +
        // custom parameters, could be load by Parameters.load() in contract
        // fee = gasPrice * gasUsage
        // e.g.
        /**
         * const parameter: Parameters = Parameters.load();
         * const customData: Uint8Array = parameter.raw();
         */
        CONTRACT_CALL(0x03);
        public final int code;

        Type(int code) {
            this.code = code;
        }

        public static final Map<Integer, Type> TYPE_MAP =
                Arrays.stream(values()).collect(Collectors.toMap(x -> x.code, Function.identity()));
    }

    @Builder
    public Transaction(HexBytes blockHash, long height, int version, int type, long createdAt, long nonce, HexBytes from, long gasPrice, long amount, HexBytes payload, HexBytes to, HexBytes signature) {
        this.blockHash = blockHash;
        this.height = height;
        this.version = version;
        this.type = type;
        this.createdAt = createdAt;
        this.nonce = nonce;
        this.from = from;
        this.gasPrice = gasPrice;
        this.amount = amount;
        this.payload = payload;
        this.to = to;
        this.signature = signature;
    }

    private HexBytes blockHash;

    private long height;

    @RLP(0)
    private int version;

    @RLP(1)
    private int type;

    @JsonSerialize(using = EpochSecondsSerializer.class)
    @JsonDeserialize(using = EpochSecondDeserializer.class)
    @RLP(2)
    private long createdAt;

    @RLP(3)
    private long nonce;

    @RLP(4)
    private HexBytes from;

    @RLP(5)
    private long gasPrice;

    @RLP(6)
    private long amount;

    @RLP(7)
    public HexBytes payload;

    @RLP(8)
    private HexBytes to;

    @RLP(9)
    private HexBytes signature;

    // generated value, no need to encode into rlp
    @Getter(AccessLevel.NONE)
    private transient HexBytes hash;

    public HexBytes getHash() {
        return getHash(false);
    }

    public HexBytes getHash(boolean forceReHash) {
        if(forceReHash || this.hash == null) {
            this.hash = HexBytes.fromBytes(
                    HashFunctions.keccak256(RLPCodec.encode(this))
            );
            return this.hash;
        }
        return this.hash;
    }

    @Override
    public Transaction clone() {
        return builder()
                .blockHash(blockHash).height(height)
                .version(version)
                .type(type).nonce(nonce)
                .createdAt(createdAt).from(from)
                .gasPrice(gasPrice).amount(amount)
                .payload(payload).to(to)
                .signature(signature).build();
    }


    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public int size() {
        return Constants.sizeOf(version) + Constants.sizeOf(type)
                + Constants.sizeOf(createdAt) + Constants.sizeOf(nonce)
                + Constants.sizeOf(gasPrice) + Constants.sizeOf(amount) +
                Stream.of(from, payload, to, signature)
                        .map(Constants::sizeOf)
                        .reduce(0, Integer::sum);
    }

    public void setBlockHash(HexBytes blockHash) {
        this.blockHash = blockHash;
        getHash(true);
    }

    public void setHeight(long height) {
        this.height = height;
        getHash(true);
    }

    public void setVersion(int version) {
        this.version = version;
        getHash(true);
    }

    public void setType(int type) {
        this.type = type;
        getHash(true);
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
        getHash(true);
    }

    public void setNonce(long nonce) {
        this.nonce = nonce;
        getHash(true);
    }

    public void setFrom(HexBytes from) {
        this.from = from;
        getHash(true);
    }

    public void setGasPrice(long gasPrice) {
        this.gasPrice = gasPrice;
        getHash(true);
    }

    public void setAmount(long amount) {
        this.amount = amount;
        getHash(true);
    }

    public void setPayload(HexBytes payload) {
        this.payload = payload;
        getHash(true);
    }

    public void setTo(HexBytes to) {
        this.to = to;
        getHash(true);
    }

    public void setSignature(HexBytes signature) {
        this.signature = signature;
        getHash(true);
    }
}

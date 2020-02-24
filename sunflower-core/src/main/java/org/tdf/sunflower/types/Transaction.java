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
import org.tdf.rlp.RLPIgnored;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
@ToString
@NoArgsConstructor
public class Transaction {
    @RLP(0)
    protected int version;
    @RLP(1)
    protected int type;
    @JsonSerialize(using = EpochSecondsSerializer.class)
    @JsonDeserialize(using = EpochSecondDeserializer.class)
    @RLP(2)
    protected long createdAt;
    @RLP(3)
    protected long nonce;
    @RLP(4)
    protected HexBytes from;
    @RLP(5)
    protected long gasPrice;
    @RLP(6)
    protected long amount;
    @RLP(7)
    protected HexBytes payload;
    @RLP(8)
    protected HexBytes to;
    @RLP(9)
    protected HexBytes signature;
    // generated value, no need to encode into rlp
    @Getter(AccessLevel.NONE)
    @RLPIgnored
    protected transient HexBytes hash;

    @Builder
    public Transaction(
            int version,
            int type, long createdAt, long nonce, HexBytes from,
            long gasPrice, long amount, HexBytes payload,
            HexBytes to, HexBytes signature
    ) {
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

    public HexBytes getHash() {
        return getHash(false);
    }

    public HexBytes getHash(boolean forceReHash) {
        if (forceReHash || this.hash == null) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return version == that.version &&
                type == that.type &&
                createdAt == that.createdAt &&
                nonce == that.nonce &&
                gasPrice == that.gasPrice &&
                amount == that.amount &&
                Objects.equals(from, that.from) &&
                Objects.equals(payload, that.payload) &&
                Objects.equals(to, that.to) &&
                Objects.equals(signature, that.signature);
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, type, createdAt, nonce, from, gasPrice, amount, payload, to, signature);
    }

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
        public static final Map<Integer, Type> TYPE_MAP =
                Arrays.stream(values()).collect(Collectors.toMap(x -> x.code, Function.identity()));
        public final int code;

        Type(int code) {
            this.code = code;
        }
    }


}

package org.tdf.sunflower.types;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.*;
import org.tdf.common.serialize.Codec;
import org.tdf.common.store.ByteArrayMapStore;
import org.tdf.common.trie.Trie;
import org.tdf.common.types.Uint256;
import org.tdf.common.util.Constants;
import org.tdf.common.util.EpochSecondDeserializer;
import org.tdf.common.util.EpochSecondsSerializer;
import org.tdf.common.util.HexBytes;
import org.tdf.rlp.RLP;
import org.tdf.rlp.RLPCodec;
import org.tdf.rlp.RLPIgnored;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.Address;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ToString
@NoArgsConstructor
public class Transaction {
    public static final int PENDING = 0;
    public static final int INCLUDED = 1;
    public static final int CONFIRMED = 2;
    public static final int DROPPED = 3;
    public static final Comparator<Transaction> NONCE_COMPARATOR = (a, b) -> {
        int cmp = a.getFrom().compareTo(b.getFrom());
        if (cmp != 0)
            return cmp;
        if (a.getNonce() != b.getNonce()) return Long.compare(a.getNonce(), b.getNonce());
        return a.getHash().compareTo(b.getHash());
    };

    public long getGasLimit() {
        return gasLimit;
    }


    @JsonIgnore
    public byte[] getSignaturePlain() {
        return RLPCodec.encode(new Object[]{
                version,
                type,
                createdAt,
                nonce,
                from,
                gasLimit,
                gasPrice,
                amount,
                payload,
                to
        });
    }

    @RLP(0)
    protected int version;
    @RLP(1)
    protected int type;
    @JsonSerialize(using = EpochSecondsSerializer.class)
    @JsonDeserialize(using = EpochSecondDeserializer.class)
    @RLP(2)
    protected long createdAt;
    @RLP(3)
    @JsonSerialize(using = ToStringSerializer.class)
    protected long nonce;

    /**
     * for coinbase, this field is null or empty bytes
     */
    @RLP(4)
    protected HexBytes from;

    @RLP(5)
    @JsonSerialize(using = ToStringSerializer.class)
    protected long gasLimit;

    @RLP(6)
    protected Uint256 gasPrice;

    @RLP(7)
    protected Uint256 amount;

    /**
     * for coinbase and transfer, this field is null or empty bytes
     */
    @RLP(8)
    protected HexBytes payload;

    /**
     * for contract deploy, this field is null or empty bytes
     */
    @RLP(9)
    protected HexBytes to;

    /**
     * not null
     */
    @RLP(10)
    protected HexBytes signature;
    // generated value, no need to encode into rlp
    @Getter(AccessLevel.NONE)
    @RLPIgnored
    protected transient HexBytes hash;

    @Builder
    public Transaction(
            int version,
            int type, long createdAt, long nonce, HexBytes from,
            long gasLimit,
            Uint256 gasPrice, Uint256 amount, HexBytes payload,
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
        this.gasLimit = gasLimit;
    }

    /**
     * get transactions root of block body, any modification of transaction or their order
     * will result in a totally different transactions root
     * light client cloud require a merkle proof to verify the existence of a transaction in the block {@link Trie#getProof(Object)}
     *
     * @param transactions list of transaction in sequential
     * @return transactions root
     */
    public static HexBytes getTransactionsRoot(List<Transaction> transactions) {
        Trie<Integer, Transaction> tmp = Trie.<Integer, Transaction>builder()
                .hashFunction(CryptoContext::hash)
                .keyCodec(Codec.newInstance(RLPCodec::encode, RLPCodec::decodeInt))
                .valueCodec(Codec.newInstance(RLPCodec::encode, x -> RLPCodec.decode(x, Transaction.class)))
                .store(new ByteArrayMapStore<>())
                .build();

        for (int i = 0; i < transactions.size(); i++) {
            tmp.put(i, transactions.get(i));
        }
        return HexBytes.fromBytes(tmp.commit());
    }

    public HexBytes getHash() {
        return getHash(false);
    }

    private HexBytes getHash(boolean forceReHash) {
        if (forceReHash || this.hash == null) {
            this.hash = HexBytes.fromBytes(
                    CryptoContext.hash(getSignaturePlain())
            );
            return this.hash;
        }
        return this.hash;
    }

    @Override
    public Transaction clone() {
        return new Transaction(version, type, createdAt, nonce, from, gasLimit, gasPrice, amount, payload, to, signature);
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public int size() {
        return Constants.sizeOf(version) + Constants.sizeOf(type)
                + Constants.sizeOf(createdAt) + Constants.sizeOf(nonce)
                + this.gasPrice.getNoLeadZeroesData().length +
                this.amount.getNoLeadZeroesData().length +
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

    public void setGasLimit(long gasLimit){
        this.gasLimit = gasLimit;
        getHash(true);
    }

    public void setGasPrice(Uint256 gasPrice) {
        this.gasPrice = gasPrice;
        getHash(true);
    }

    public void setAmount(Uint256 amount) {
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

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public Uint256 getFee(){
        if(type == Type.COIN_BASE.code)
            return Uint256.ZERO;
        if(type == Type.TRANSFER.code)
            return  Uint256.of(10L).mul(getGasPrice());
        return Uint256.ZERO;
    }


    @Override
    public int hashCode() {
        return Objects.hash(version, type, createdAt, nonce, from, gasPrice, amount, payload, to, signature);
    }

    public enum Type {
        // coinbase transaction has code 0, may trigger bios
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

    /**
     * get contract address, contract address = hash(rlp(from, nonce))
     *
     * @return contact address
     */
    public HexBytes createContractAddress() {
        if (type != Type.CONTRACT_DEPLOY.code) throw new RuntimeException("not a contract deploy transaction");
        return createContractAddress(getFromAddress(), nonce);
    }

    /**
     * get contract address, contract address = hash(rlp(from, nonce))
     *
     * @return contact address
     */
    public static HexBytes createContractAddress(HexBytes address, long nonce) {
        byte[] bytes = CryptoContext.hash(RLPCodec.encode(new Object[]{address, nonce}));
        HexBytes ret = HexBytes.fromBytes(bytes);
        return ret.slice(ret.size() - Account.ADDRESS_SIZE, ret.size());
    }

    public int getVersion() {
        return version;
    }

    public int getType() {
        return type;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getNonce() {
        return nonce;
    }

    public HexBytes getFrom() {
        return from == null ? HexBytes.EMPTY : from;
    }

    @JsonIgnore
    public HexBytes getFromAddress() {
        if (getFrom().isEmpty()) throw new RuntimeException("from not found: coinbase transaction");
        return Address.fromPublicKey(getFrom());
    }

    public Uint256 getGasPrice() {
        return gasPrice;
    }

    public Uint256 getAmount() {
        return amount;
    }

    public HexBytes getPayload() {
        return payload == null ? HexBytes.EMPTY : payload;
    }

    public HexBytes getTo() {
        return to == null ? HexBytes.EMPTY : to;
    }

    public HexBytes getSignature() {
        return signature == null ? HexBytes.EMPTY : signature;
    }

    public ValidateResult basicValidate() {
        if(gasLimit < 0)
            throw new RuntimeException("negative gas limit");
        if (amount == null || gasPrice == null)
            return ValidateResult.fault("missing amount or gas price");
        if (!Type.TYPE_MAP.containsKey(type))
            return ValidateResult.fault("unknown transaction type " + type + " of " + getHash());
        if (type != Type.COIN_BASE.code && (signature == null || signature.isEmpty()))
            return ValidateResult.fault("missing signature of transaction " + getHash());
        if (type == Type.COIN_BASE.code && !getFrom().isEmpty())
            return ValidateResult.fault("\"from\" of coinbase transaction " + getHash() + " should be empty");
        if (type == Type.CONTRACT_DEPLOY.code && !getTo().isEmpty()) {
            return ValidateResult.fault("\"to\" of contract deploy transaction " + getHash() + " should be empty");
        }
        if (type == Type.CONTRACT_CALL.code || type == Type.TRANSFER.code) {
            if (getFrom().isEmpty() || getTo().isEmpty())
                return ValidateResult.fault("\"from\" or \"to\" of transaction " + getHash() + " is empty");
        }
        if (type == Type.CONTRACT_CALL.code || type == Type.CONTRACT_DEPLOY.code) {
            if (getPayload().isEmpty())
                return ValidateResult.fault("missing payload of transaction " + getHash());
        }
        if (type == Type.TRANSFER.code) {
            if (!getPayload().isEmpty())
                return ValidateResult.fault("payload of transaction " + getHash() + " should be empty");
        }
        if (type != Type.COIN_BASE.code && !CryptoContext.verify(from.getBytes(), getSignaturePlain(), signature.getBytes())) {
            return ValidateResult.fault("verify signature failed " + getHash());
        }
        return ValidateResult.success();
    }
}

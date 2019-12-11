package org.tdf.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.*;
import org.tdf.rlp.RLP;
import org.tdf.util.EpochSecondDeserializer;
import org.tdf.util.EpochSecondsSerializer;

import java.util.stream.Stream;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction implements Cloneable<Transaction> {
    public enum Type{
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

        Type(int code){
            this.code = code;
        }
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

    @RLP(10)
    private HexBytes hash;

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

}

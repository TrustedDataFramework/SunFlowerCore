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
import org.tdf.rlp.RLP;
import org.tdf.rlp.RLPIgnored;

import java.util.stream.Stream;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Header implements Chained {
    /**
     * magic version number
     */
    @RLP(0)
    private int version;

    /**
     * hash of parent block
     */
    @RLP(1)
    private HexBytes hashPrev;

    /**
     * root hash of transaction trie
     */
    @RLP(2)
    private HexBytes transactionsRoot;

    /**
     * root hash of state trie
     */
    @RLP(3)
    private HexBytes stateRoot;

    /**
     * height of current header
     */
    @RLP(4)
    private long height;

    /**
     * unix epoch when the block mined
     */
    @JsonSerialize(using = EpochSecondsSerializer.class)
    @JsonDeserialize(using = EpochSecondDeserializer.class)
    @RLP(5)
    private long createdAt;

    /**
     * custom data
     */
    @RLP(6)
    private HexBytes payload;


    /**
     * hash of the block, usually set only once
     */
    @RLPIgnored
    private HexBytes hash;

    @Override
    public Header clone() {
        return builder().version(version)
                .hashPrev(hashPrev).transactionsRoot(transactionsRoot)
                .height(height).createdAt(createdAt)
                .payload(payload).build();
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public int size() {
        return Constants.sizeOf(version) + Constants.sizeOf(height) +
                Constants.sizeOf(createdAt) +
                Stream.of(hashPrev, transactionsRoot, payload, hash)
                        .map(Constants::sizeOf)
                        .reduce(0, Integer::sum);
    }
}

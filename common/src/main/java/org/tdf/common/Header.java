package org.tdf.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.*;
import org.tdf.serialize.RLP;
import org.tdf.util.EpochSecondDeserializer;
import org.tdf.util.EpochSecondsSerializer;

import java.util.stream.Stream;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Header implements Cloneable<Header>, Chained {
    @RLP(0)
    private int version;

    @RLP(1)
    private HexBytes hashPrev;

    @RLP(2)
    private HexBytes merkleRoot;

    @RLP(3)
    private long height;

    @JsonSerialize(using = EpochSecondsSerializer.class)
    @JsonDeserialize(using = EpochSecondDeserializer.class)
    @RLP(4)
    private long createdAt;

    @RLP(5)
    private HexBytes payload;

    @RLP(6)
    private HexBytes hash;

    @Override
    public Header clone() {
        return builder().version(version)
                .hashPrev(hashPrev).merkleRoot(merkleRoot)
                .height(height).createdAt(createdAt)
                .payload(payload).build();
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public int size() {
        return Constants.sizeOf(version) + Constants.sizeOf(height) +
                Constants.sizeOf(createdAt) +
                Stream.of(hashPrev, merkleRoot, payload, hash)
                        .map(Constants::sizeOf)
                        .reduce(0, Integer::sum);
    }
}

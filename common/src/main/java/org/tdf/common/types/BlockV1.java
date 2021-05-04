package org.tdf.common.types;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.tdf.common.util.EpochSecondDeserializer;
import org.tdf.common.util.EpochSecondsSerializer;
import org.tdf.common.util.HexBytes;
import org.tdf.rlp.RLP;

import java.util.ArrayList;
import java.util.List;

public class BlockV1 implements Chained {


    // extend from header
    @Getter
    @JsonIgnore
    @RLP(0)
    protected HeaderV1 header;


    @Getter
    @Setter
    @RLP(1)
    protected List<TransactionV1> body;

    public BlockV1() {
        header = new HeaderV1();
        body = new ArrayList<>();
    }

    public BlockV1(@NonNull HeaderV1 header) {
        this.header = header;
        body = new ArrayList<>();
    }

    @JsonSerialize(using = EpochSecondsSerializer.class)
    @JsonDeserialize(using = EpochSecondDeserializer.class)
    public long getCreatedAt() {
        return header.getCreatedAt();
    }

    public void setCreatedAt(long createdAt) {
        header.setCreatedAt(createdAt);
    }


    // serialization only
    @JsonProperty(access = JsonProperty.Access.READ_ONLY, value = "size")
    public int size() {
        return 0;
    }

    public int getVersion() {
        return header.getVersion();
    }

    public void setVersion(int version) {
        header.setVersion(version);
    }

    public HexBytes getHashPrev() {
        return header.getHashPrev();
    }

    public void setHashPrev(HexBytes hashPrev) {
        header.setHashPrev(hashPrev);
    }

    public HexBytes getTransactionsRoot() {
        return header.getTransactionsRoot();
    }

    public void setTransactionsRoot(HexBytes transactionsRoot) {
        header.setTransactionsRoot(transactionsRoot);
    }


    public HexBytes getStateRoot() {
        return header.getStateRoot();
    }

    public void setStateRoot(HexBytes stateRoot) {
        header.setStateRoot(stateRoot);
    }

    public long getHeight() {
        return header.getHeight();
    }

    public void setHeight(long height) {
        header.setHeight(height);
    }

    public HexBytes getPayload() {
        return header.getPayload();
    }

    public void setPayload(HexBytes payload) {
        header.setPayload(payload);
    }

    public HexBytes getHash() {
        return header.getHash();
    }
}

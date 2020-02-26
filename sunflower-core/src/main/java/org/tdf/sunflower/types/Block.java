package org.tdf.sunflower.types;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.tdf.common.types.Chained;
import org.tdf.common.util.EpochSecondDeserializer;
import org.tdf.common.util.EpochSecondsSerializer;
import org.tdf.common.util.HexBytes;
import org.tdf.rlp.RLP;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Block implements Chained {
    // extend from header
    @Getter
    @JsonIgnore
    @RLP(0)
    protected Header header;


    @Getter
    @Setter
    @RLP(1)
    protected List<Transaction> body;

    public Block() {
        header = new Header();
        body = new ArrayList<>();
    }

    public Block(@NonNull Header header) {
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

    public Block clone() {
        Block b = new Block(header.clone());
        b.setBody(body.stream().map(Transaction::clone).collect(Collectors.toList()));
        return b;
    }

    // serialization only
    @JsonProperty(access = JsonProperty.Access.READ_ONLY, value = "size")
    public int size() {
        return header.size() + bodySize();
    }

    private int bodySize() {
        return body == null ? 0 : body.stream()
                .map(Transaction::size)
                .reduce(0, Integer::sum);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Block block = (Block) o;
        return Objects.equals(header, block.header) &&
                Objects.equals(body, block.body);
    }

    @Override
    public int hashCode() {
        return Objects.hash(header, body);
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

    public void resetTransactionsRoot() {
        setTransactionsRoot(Transaction.getTransactionsRoot(getBody()));
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

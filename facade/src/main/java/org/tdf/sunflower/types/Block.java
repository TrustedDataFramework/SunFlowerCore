package org.tdf.sunflower.types;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.NonNull;
import org.tdf.common.types.Chained;
import org.tdf.common.util.HexBytes;
import org.tdf.rlpstream.RlpCreator;
import org.tdf.rlpstream.RlpProps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;


@RlpProps({"header", "body"})
public class Block implements Chained {
    public static final Comparator<Block> FAT_COMPARATOR = (a, b) -> {
        if (a.getHeight() != b.getHeight())
            return Long.compare(a.getHeight(), b.getHeight());
        if (a.body.size() != b.body.size()) {
            return -Integer.compare(a.getBody().size(), b.getBody().size());
        }
        return a.getHash().compareTo(b.getHash());
    };

    public static final Comparator<Block> BEST_COMPARATOR = (a, b) -> {
        if (a.getHeight() != b.getHeight())
            return Long.compare(a.getHeight(), b.getHeight());
        if (a.body.size() != b.body.size()) {
            return Integer.compare(a.getBody().size(), b.getBody().size());
        }
        return a.getHash().compareTo(b.getHash());
    };

    // extend from header
    @JsonIgnore
    protected Header header;

    protected List<Transaction> body;

    @RlpCreator
    public Block(Header header, Transaction[] body) {
        this.header = header;
        this.body = Arrays.asList(body);
    }

    public Block() {
        header = new Header();
        body = new ArrayList<>();
    }

    public Block(@NonNull Header header) {
        this.header = header;
        body = new ArrayList<>();
    }

    public long getCreatedAt() {
        return header.getCreatedAt();
    }

    public HexBytes getExtraData() {
        return header.getExtraData();
    }

    public HexBytes getMixHash() {
        return header.getMixHash();
    }

    public HexBytes getNonce() {
        return header.getNonce();
    }

    @Override
    public HexBytes getHash() {
        return header.getHash();
    }

    public void setHashPrev(HexBytes hashPrev) {
        header.setHashPrev(hashPrev);
    }

    public void setTransactionsRoot(HexBytes transactionsRoot) {
        header.setTransactionsRoot(transactionsRoot);
    }

    public void setStateRoot(HexBytes stateRoot) {
        header.setStateRoot(stateRoot);
    }

    public void setHeight(long height) {
        header.setHeight(height);
    }

    public void setCreatedAt(long createdAt) {
        header.setCreatedAt(createdAt);
    }

    public void setCoinbase(HexBytes coinbase) {
        header.setCoinbase(coinbase);
    }

    public void setGasLimit(HexBytes gasLimit) {
        header.setGasLimit(gasLimit);
    }

    public void setGasUsed(long gasUsed) {
        header.setGasUsed(gasUsed);
    }


    public void resetTransactionsRoot() {
        setTransactionsRoot(
            Transaction.calcTxTrie(getBody())
        );
    }

    @Override
    public HexBytes getHashPrev() {
        return header.getHashPrev();
    }

    public HexBytes getUnclesHash() {
        return header.getUnclesHash();
    }

    public HexBytes getCoinbase() {
        return header.getCoinbase();
    }

    public HexBytes getStateRoot() {
        return header.getStateRoot();
    }

    public HexBytes getTransactionsRoot() {
        return header.getTransactionsRoot();
    }

    public HexBytes getReceiptTrieRoot() {
        return header.getReceiptTrieRoot();
    }

    public HexBytes getLogsBloom() {
        return header.getLogsBloom();
    }

    public HexBytes getDifficulty() {
        return header.getDifficulty();
    }

    public long getHeight() {
        return header.getHeight();
    }

    public HexBytes getGasLimit() {
        return header.getGasLimit();
    }

    public long getGasUsed() {
        return header.getGasUsed();
    }

    public static Header.HeaderBuilder builder() {
        return Header.builder();
    }

    public void setExtraData(HexBytes extraData) {
        header.setExtraData(extraData);
    }

    public void setMixHash(HexBytes mixHash) {
        header.setMixHash(mixHash);
    }

    public void setNonce(HexBytes nonce) {
        header.setNonce(nonce);
    }


    public void setReceiptTrieRoot(HexBytes receiptTrieRoot) {
        header.setReceiptTrieRoot(receiptTrieRoot);
    }

    public void setLogsBloom(HexBytes logsBloom) {
        header.setLogsBloom(logsBloom);
    }

    public Header getHeader() {
        return this.header;
    }

    public List<Transaction> getBody() {
        return this.body;
    }

    public void setBody(List<Transaction> body) {
        this.body = body;
    }
}

package org.tdf.sunflower.controller;

import org.tdf.rlp.RLPCodec;
import org.tdf.rlp.RLPElement;
import org.tdf.rlp.RLPItem;
import org.tdf.rlp.RLPList;
import org.tdf.sunflower.types.Transaction;
import org.tdf.sunflower.types.TransactionReceipt;

import java.math.BigInteger;
import java.util.Arrays;

public class TransactionInfo {

    TransactionReceipt receipt;
    byte[] blockHash;
    // user for pending transaction
    byte[] parentBlockHash;
    int index;

    public TransactionInfo(TransactionReceipt receipt, byte[] blockHash, int index) {
        this.receipt = receipt;
        this.blockHash = blockHash;
        this.index = index;
    }

    /**
     * Creates a pending tx info
     */
    public TransactionInfo(TransactionReceipt receipt) {
        this.receipt = receipt;
    }

    public TransactionInfo(byte[] rlp) {
        RLPList params = RLPElement.fromEncoded(rlp).asRLPList();
        RLPList txInfo = (RLPList) params.get(0);
        RLPList receiptRLP = (RLPList) txInfo.get(0);
        RLPItem blockHashRLP = (RLPItem) txInfo.get(1);
        RLPItem indexRLP = (RLPItem) txInfo.get(2);

        receipt = new TransactionReceipt(receiptRLP.asBytes());
        blockHash = blockHashRLP.asBytes();
        if (indexRLP.asBytes() == null)
            index = 0;
        else
            index = new BigInteger(1, indexRLP.asBytes()).intValue();
    }

    public void setTransaction(Transaction tx) {
        receipt.setTransaction(tx);
    }

    /* [receipt, blockHash, index] */
    public byte[] getEncoded() {

        byte[] receiptRLP = this.receipt.getEncoded();
        byte[] blockHashRLP = RLPCodec.encodeBytes(blockHash);
        byte[] indexRLP = RLPCodec.encodeInt(index);

        byte[] rlpEncoded = RLPCodec.encodeElements(Arrays.asList(receiptRLP, blockHashRLP, indexRLP));

        return rlpEncoded;
    }

    public TransactionReceipt getReceipt() {
        return receipt;
    }

    public byte[] getBlockHash() {
        return blockHash;
    }

    public byte[] getParentBlockHash() {
        return parentBlockHash;
    }

    public void setParentBlockHash(byte[] parentBlockHash) {
        this.parentBlockHash = parentBlockHash;
    }

    public int getIndex() {
        return index;
    }

    public boolean isPending() {
        return blockHash == null;
    }
}


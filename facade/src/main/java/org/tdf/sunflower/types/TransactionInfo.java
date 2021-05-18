package org.tdf.sunflower.types;

import org.tdf.common.util.HexBytes;
import org.tdf.rlpstream.*;

import java.util.Arrays;


public class TransactionInfo implements RlpEncodable {
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

    @RlpCreator
    public static Transaction fromRlpStream(byte[] bin, long streamId) {
        return new Transaction(RlpStream.rawOf(bin, streamId));
    }

    public TransactionInfo(byte[] rlp) {
        RlpList txInfo = Rlp.decodeList(rlp);
        byte[] receiptRLP = txInfo.rawAt(0);

        blockHash = txInfo.bytesAt(1);
        index = Rlp.decodeInt(txInfo.rawAt(2));
        receipt = new TransactionReceipt(receiptRLP);
    }

    public void setTransaction(Transaction tx) {
        receipt.setTransaction(tx);
    }

    /* [receipt, blockHash, index] */
    public byte[] getEncoded() {

        byte[] receiptRLP = this.receipt.getEncoded();
        byte[] blockHashRLP = Rlp.encodeBytes(blockHash);
        byte[] indexRLP = Rlp.encodeInt(index);

        return Rlp.encodeElements(Arrays.asList(receiptRLP, blockHashRLP, indexRLP));
    }

    public TransactionReceipt getReceipt() {
        return receipt;
    }

    public byte[] getBlockHash() {
        return blockHash;
    }


    public HexBytes getBlockHashHex() {
        return HexBytes.fromBytes(blockHash);
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


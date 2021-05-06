package org.tdf.sunflower.types;

import lombok.NonNull;
import org.tdf.rlp.*;

import java.math.BigInteger;
import java.util.Arrays;

@RLPEncoding(TransactionInfo.TransactionInfoEncoder.class)
@RLPDecoding(TransactionInfo.TransactionInfoDecoder.class)
public class TransactionInfo {
    public static class TransactionInfoEncoder implements RLPEncoder<TransactionInfo> {

        @Override
        public RLPElement encode(@NonNull TransactionInfo o) {
            return RLPElement.fromEncoded(o.getEncoded());
        }
    }

    public static class TransactionInfoDecoder implements RLPDecoder<TransactionInfo> {

        @Override
        public TransactionInfo decode(@NonNull RLPElement element) {
            return new TransactionInfo(element.getEncoded());
        }
    }


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
        RLPList txInfo = RLPElement.fromEncoded(rlp).asRLPList();
        RLPList receiptRLP = txInfo.get(0).asRLPList();
        RLPItem blockHashRLP = txInfo.get(1).asRLPItem();
        RLPItem indexRLP = txInfo.get(2).asRLPItem();

        receipt = new TransactionReceipt(receiptRLP.getEncoded());
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


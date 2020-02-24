package org.tdf.sunflower.types;

import lombok.NonNull;
import org.tdf.common.util.HexBytes;
import org.tdf.rlp.*;

@RLPEncoding(UnmodifiableTransaction.Encoder.class)
@RLPDecoding(UnmodifiableTransaction.Decoder.class)
public class UnmodifiableTransaction extends Transaction {
    private Transaction modifiable;

    private UnmodifiableTransaction(
            Transaction tx
    ) {
        super(
                tx.version, tx.type, tx.createdAt,
                tx.nonce, tx.from, tx.gasPrice,
                tx.amount, tx.payload, tx.to, tx.signature
        );
        this.hash = super.getHash();
        this.modifiable =
                new Transaction(
                        version, type, createdAt,
                        nonce, from, gasPrice,
                        amount, payload, to, signature
                );
    }

    public UnmodifiableTransaction(
            int version, int type, long createdAt,
            long nonce, HexBytes from, long gasPrice,
            long amount, HexBytes payload, HexBytes to,
            HexBytes signature
    ) {
        super(version, type, createdAt, nonce, from, gasPrice, amount, payload, to, signature);
        this.hash = super.getHash();
        this.modifiable = new Transaction(version, type, createdAt, nonce, from, gasPrice, amount, payload, to, signature);
    }

    public static UnmodifiableTransaction of(Transaction tx) {
        if (tx instanceof UnmodifiableTransaction) return ((UnmodifiableTransaction) tx);
        return new UnmodifiableTransaction(tx);
    }

    @Override
    public void setVersion(int version) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setType(int type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setCreatedAt(long createdAt) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setNonce(long nonce) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setFrom(HexBytes from) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setGasPrice(long gasPrice) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setAmount(long amount) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setPayload(HexBytes payload) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setTo(HexBytes to) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setSignature(HexBytes signature) {
        throw new UnsupportedOperationException();
    }

    static class Encoder implements RLPEncoder<UnmodifiableTransaction> {
        @Override
        public RLPElement encode(@NonNull UnmodifiableTransaction tx) {
            return RLPElement.readRLPTree(tx.modifiable);
        }
    }

    static class Decoder implements RLPDecoder<UnmodifiableTransaction> {
        @Override
        public UnmodifiableTransaction decode(@NonNull RLPElement el) {
            return new UnmodifiableTransaction(el.as(Transaction.class));
        }
    }
}

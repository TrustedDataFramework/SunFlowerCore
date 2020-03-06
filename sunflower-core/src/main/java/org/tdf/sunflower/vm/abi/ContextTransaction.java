package org.tdf.sunflower.vm.abi;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.types.Transaction;

@Getter
@AllArgsConstructor
public class ContextTransaction {
    private int type;
    private long createdAt;
    private long nonce;


    private HexBytes from;
    private long gasPrice;
    private long amount;

    private HexBytes payload;

    private HexBytes to;

    private HexBytes signature;

    private HexBytes hash;

    public ContextTransaction(Transaction transaction){
        this(
                transaction.getType(),
                transaction.getCreatedAt(), transaction.getNonce(), transaction.getFrom(),
                transaction.getGasPrice(), transaction.getAmount(), transaction.getPayload(),
                transaction.getTo(), transaction.getSignature(), transaction.getHash()
        );
    }
}

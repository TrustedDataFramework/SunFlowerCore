package org.tdf.sunflower.vm.abi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.types.Transaction;
import org.tdf.sunflower.util.BytesReader;

import java.nio.charset.StandardCharsets;


@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Context {
    public static Context disabled(){
        return Context.builder().build();
    }

    public static String getMethod(byte[] parameters){
        BytesReader reader = new BytesReader(parameters);
        int len = reader.read();
        return new String(reader.read(len), StandardCharsets.US_ASCII);
    }

    public static Context fromTransaction(Header header, Transaction transaction) {
        ContextBuilder builder = builder();
        if (transaction.getType() == Transaction.Type.CONTRACT_DEPLOY.code) {
            builder.method("init");
        } else {
            builder.method(getMethod(transaction.getPayload().getBytes()));
        }
        builder.sender(transaction.getFromAddress())
                .recipient(transaction.getTo())
                .transactionHash(transaction.getHash())
                .gasPrice(transaction.getGasPrice())
                .parentBlockHash(header.getHashPrev())
                .blockHeight(header.getHeight())
                .payload(transaction.getPayload().getBytes())
                .transactionTimestamp(transaction.getCreatedAt())
                .nonce(transaction.getNonce())
                .signature(transaction.getSignature())
                .amount(transaction.getAmount())
                .available(true)
                .blockTimestamp(header.getCreatedAt())
        ;
        return builder.build();
    }

    private HexBytes transactionHash;

    private String method;

    private HexBytes sender;

    private HexBytes recipient;

    private long amount;

    private long gasPrice;

    private long blockTimestamp;

    private long transactionTimestamp;

    private HexBytes parentBlockHash;

    private byte[] payload;

    private long blockHeight;

    private long nonce;

    private HexBytes signature;

    private boolean available;

    private HexBytes createdBy;

    private HexBytes contractAddress;
}

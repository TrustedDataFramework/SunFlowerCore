package org.tdf.sunflower.vm.abi;

import lombok.Builder;
import lombok.Data;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.types.Transaction;
import org.tdf.sunflower.util.BytesReader;

import java.nio.charset.StandardCharsets;


@Builder
@Data
public class Context {
    public static Context disabled(){
        return Context.builder().build();
    }


    public static Context fromTransaction(Header header, Transaction transaction) {
        BytesReader reader = new BytesReader(transaction.getPayload().getBytes());
        ContextBuilder builder = builder();
        if (transaction.getType() == Transaction.Type.CONTRACT_DEPLOY.code) {
            builder.method("init");
        } else {
            int len = reader.read();
            builder.method(new String(reader.read(len), StandardCharsets.US_ASCII));
        }
        builder.sender(transaction.getFrom())
                .recipient(transaction.getTo())
                .transactionHash(transaction.getHash())
                .gasPrice(transaction.getGasPrice())
                .parentBlockHash(header.getHashPrev())
                .blockHeight(header.getHeight())
                .payload(reader.readAll())
                .transactionTimestamp(transaction.getCreatedAt())
                .nonce(transaction.getNonce())
                .signature(transaction.getSignature())
                .available(true)
                .blockTimestamp(header.getCreatedAt());
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

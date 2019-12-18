package org.tdf.sunflower.vm.abi;

import lombok.Builder;
import lombok.Getter;
import org.tdf.sunflower.types.Block;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.types.Transaction;
import org.tdf.sunflower.util.BytesReader;

import java.nio.charset.StandardCharsets;


@Getter
@Builder
public class Context {

    public static Context fromTransaction(Block block, Transaction transaction) {
        BytesReader reader = new BytesReader(transaction.payload.getBytes());
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
                .parentBlockHash(block.getHashPrev())
                .blockHeight(block.getHeight())
                .payload(reader.readAll())
                .transactionTimestamp(transaction.getCreatedAt())
                .blockTimestamp(block.getCreatedAt())
                .gasLimit(3);
        return builder.build();
    }

    private HexBytes transactionHash;

    private String method;

    private HexBytes sender;

    private HexBytes recipient;

    private long amount;

    private long fee;

    private long gasPrice;

    private long gasLimit;

    private long blockTimestamp;

    private long transactionTimestamp;

    private HexBytes parentBlockHash;

    private byte[] payload;

    private long blockHeight;

}

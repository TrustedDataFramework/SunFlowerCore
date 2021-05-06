package org.tdf.sunflower.controller;

import lombok.Value;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.Transaction;

import static org.tdf.sunflower.controller.TypeConverter.toJsonHex;
import static org.tdf.sunflower.controller.TypeConverter.toJsonHexNumber;

@Value
public class TransactionResultDTO {

    public String hash;
    public String nonce;
    public String blockHash;
    public String blockNumber;
    public String transactionIndex;

    public String from;
    public String to;
    public String gas;
    public String gasPrice;
    public String value;
    public String input;

    public TransactionResultDTO(Block b, int index, Transaction tx) {
        hash = toJsonHex(tx.getHash());
        nonce = toJsonHex(tx.getNonce());
        blockHash = toJsonHex(b.getHash().getBytes());
        blockNumber = toJsonHex(b.getHeight());
        transactionIndex = toJsonHex(index);
        from = toJsonHex(tx.getSender());
        to = toJsonHex(tx.getReceiveAddress());
        gas = toJsonHex(tx.getGasLimit());
        gasPrice = toJsonHex(tx.getGasPrice());
        value = toJsonHexNumber(tx.getValue());
        input = toJsonHex(tx.getData());
    }

    @Override
    public String toString() {
        return "TransactionResultDTO{" +
            "hash='" + hash + '\'' +
            ", nonce='" + nonce + '\'' +
            ", blockHash='" + blockHash + '\'' +
            ", blockNumber='" + blockNumber + '\'' +
            ", transactionIndex='" + transactionIndex + '\'' +
            ", from='" + from + '\'' +
            ", to='" + to + '\'' +
            ", gas='" + gas + '\'' +
            ", gasPrice='" + gasPrice + '\'' +
            ", value='" + value + '\'' +
            ", input='" + input + '\'' +
            '}';
    }
}

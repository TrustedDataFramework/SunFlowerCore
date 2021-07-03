package org.tdf.sunflower.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.LogInfo;
import org.tdf.sunflower.types.TransactionInfo;
import org.tdf.sunflower.types.TransactionReceipt;

import static org.tdf.sunflower.controller.TypeConverter.toJsonHex;

public class TransactionReceiptDTO {

    public final String transactionHash;          // hash of the transaction.
    public final String transactionIndex;         // integer of the transactions index position in the block.
    public final String blockHash;                // hash of the block where this transaction was in.
    public final String blockNumber;              // block number where this transaction was in.
    public final String from;                     // 20 Bytes - address of the sender.
    public final String to;                       // 20 Bytes - address of the receiver. null when its a contract creation transaction.
    public final String cumulativeGasUsed;        // The total amount of gas used when this transaction was executed in the block.
    public final String gasUsed;                  // The amount of gas used by this specific transaction alone.
    public final String contractAddress;          // The contract address created, if the transaction was a contract creation, otherwise  null .
    public final JsonRpc.LogFilterElement[] logs;         // Array of log objects, which this transaction generated.
    public final String logsBloom;                       // 256 Bytes - Bloom filter for light clients to quickly retrieve related logs.
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public final String status;  //  either 1 (success) or 0 (failure) (post Byzantium)

    public TransactionReceiptDTO(Block block, TransactionInfo txInfo) {
        TransactionReceipt receipt = txInfo.getReceipt();

        transactionHash = toJsonHex(receipt.getTransaction().getHash());
        transactionIndex = toJsonHex(txInfo.getIndex());
        cumulativeGasUsed = toJsonHex(receipt.getCumulativeGas(), "0x0");
        gasUsed = toJsonHex(receipt.getGasUsed(), "0x0");
        contractAddress = toJsonHex(receipt.getTransaction().getContractAddress());
        from = toJsonHex(receipt.getTransaction().getSender());
        to = toJsonHex(receipt.getTransaction().getReceiveAddress());
        logs = new JsonRpc.LogFilterElement[receipt.getLogInfoList().size()];

        if (block != null) {
            blockNumber = toJsonHex(block.getHeight());
            blockHash = toJsonHex(txInfo.getBlockHash());
        } else {
            blockNumber = null;
            blockHash = null;
        }

        for (int i = 0; i < logs.length; i++) {
            LogInfo logInfo = receipt.getLogInfoList().get(i);
            logs[i] = new JsonRpc.LogFilterElement(logInfo, block, txInfo.getIndex(),
                txInfo.getReceipt().getTransaction(), i);
        }
        logsBloom = toJsonHex(receipt.getBloomFilter().getData());
        status = "0x1";

//        if (receipt.hasTxStatus()) { // post Byzantium
//            root = null;
//            status = receipt.isTxStatusOK() ? "0x1" : "0x0";
//            root = toJsonHex(receipt.getPostTxState());
//        } else { // pre Byzantium
//            status = null;
//        }
    }

    public String getTransactionHash() {
        return this.transactionHash;
    }

    public String getTransactionIndex() {
        return this.transactionIndex;
    }

    public String getBlockHash() {
        return this.blockHash;
    }

    public String getBlockNumber() {
        return this.blockNumber;
    }

    public String getFrom() {
        return this.from;
    }

    public String getTo() {
        return this.to;
    }

    public String getCumulativeGasUsed() {
        return this.cumulativeGasUsed;
    }

    public String getGasUsed() {
        return this.gasUsed;
    }

    public String getContractAddress() {
        return this.contractAddress;
    }

    public JsonRpc.LogFilterElement[] getLogs() {
        return this.logs;
    }

    public String getLogsBloom() {
        return this.logsBloom;
    }

    public String getStatus() {
        return this.status;
    }


    protected boolean canEqual(final Object other) {
        return other instanceof TransactionReceiptDTO;
    }
}



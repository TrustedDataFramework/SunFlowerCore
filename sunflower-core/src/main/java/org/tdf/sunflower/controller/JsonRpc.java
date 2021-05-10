package org.tdf.sunflower.controller;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.LogInfo;
import org.tdf.sunflower.types.Transaction;
import org.tdf.sunflower.vm.CallData;
import org.tdf.sunflower.vm.CallType;

import java.util.Arrays;
import java.util.List;

import static org.tdf.sunflower.controller.TypeConverter.*;

public interface JsonRpc {

    String web3_clientVersion();

    String web3_sha3(String data) throws Exception;

    String net_version();

    String eth_chainId();

    String net_peerCount();

    boolean net_listening();

    String eth_protocolVersion();

    Object eth_syncing();

    String eth_coinbase();

    boolean eth_mining();

    String eth_hashrate();

    String eth_gasPrice();

    String[] eth_accounts();

    String eth_blockNumber();

    String eth_getBalance(String address, String block) throws Exception;

    String eth_getLastBalance(String address) throws Exception;

    String eth_getStorageAt(String address, String storageIdx, String blockId) throws Exception;

    String eth_getTransactionCount(String address, String blockId) throws Exception;

    String eth_getBlockTransactionCountByHash(String blockHash) throws Exception;

    String eth_getBlockTransactionCountByNumber(String bnOrId) throws Exception;

    String eth_getUncleCountByBlockHash(String blockHash) throws Exception;

    String eth_getUncleCountByBlockNumber(String bnOrId) throws Exception;

    String eth_getCode(String addr, String bnOrId) throws Exception;

    String eth_sign(String addr, String data) throws Exception;

    String eth_sendTransaction(CallArguments transactionArgs) throws Exception;

    String eth_sendRawTransaction(String rawData) throws Exception;

    String eth_call(CallArguments args, String bnOrId) throws Exception;

    String eth_estimateGas(CallArguments args) throws Exception;

    BlockResult eth_getBlockByHash(String blockHash, Boolean fullTransactionObjects) throws Exception;

    BlockResult eth_getBlockByNumber(String bnOrId, Boolean fullTransactionObjects) throws Exception;

    TransactionResultDTO eth_getTransactionByHash(String transactionHash) throws Exception;

    TransactionResultDTO eth_getTransactionByBlockHashAndIndex(String blockHash, String index) throws Exception;

    TransactionResultDTO eth_getTransactionByBlockNumberAndIndex(String bnOrId, String index) throws Exception;

    TransactionReceiptDTO eth_getTransactionReceipt(String transactionHash) throws Exception;

    TransactionReceiptDTOExt ethj_getTransactionReceipt(String transactionHash) throws Exception;

    BlockResult eth_getUncleByBlockHashAndIndex(String blockHash, String uncleIdx) throws Exception;

    BlockResult eth_getUncleByBlockNumberAndIndex(String blockId, String uncleIdx) throws Exception;


    String eth_newFilter(FilterRequest fr) throws Exception;

    String eth_newBlockFilter();

    String eth_newPendingTransactionFilter();

    boolean eth_uninstallFilter(String id);

    Object[] eth_getFilterChanges(String id);

    Object[] eth_getFilterLogs(String id);

    Object[] eth_getLogs(FilterRequest fr) throws Exception;

//    String eth_resend();
//    String eth_pendingTransactions();

    List<Object> eth_getWork();

//    String eth_newFilter(String fromBlock, String toBlock, String address, String[] topics) throws Exception;

    boolean eth_submitWork(String nonce, String header, String digest) throws Exception;

    boolean eth_submitHashrate(String hashrate, String id);


    String personal_signAndSendTransaction(CallArguments tx, String password);
//    boolean miner_startAutoDAG();
//    boolean miner_stopAutoDAG();
//    boolean miner_makeDAG();
//    String miner_hashrate();

//    String debug_printBlock();
//    String debug_getBlockRlp();
//    String debug_setHead();
//    String debug_processBlock();
//    String debug_seedHash();
//    String debug_dumpBlock();
//    String debug_metrics();

    @Value
    @AllArgsConstructor
    @ToString
    class SyncingResult {
        private final String startingBlock;
        private final String currentBlock;
        private final String highestBlock;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    class CallArguments {
        public String from;
        public String to;
        public String gas;
        public String gasPrice;
        public String value;
        public String data; // compiledCode
        public String nonce;

        @JsonIgnore
        @SneakyThrows
        public CallData toCallData() {
            CallData data = CallData.empty();
            if (from != null && !from.isEmpty()) {
                data.setCaller(jsonHexToHexBytes(from));
                data.setOrigin(jsonHexToHexBytes(from));
            }
            if (to != null && !to.isEmpty()) {
                data.setTxTo(jsonHexToHexBytes(to));
                data.setTo(jsonHexToHexBytes(to));
                data.setCallType(CallType.CALL);
            } else {
                data.setCallType(CallType.CREATE);
            }
            if (gas != null && !gas.isEmpty()) {
                data.setGasLimit(
                    jsonHexToU256(gas)
                );
            }
            if (gasPrice != null && !gasPrice.isEmpty()) {
                data.setGasPrice(
                    jsonHexToU256(gasPrice)
                );
            }
            if (value != null && !value.isEmpty()) {
                data.setValue(
                    jsonHexToU256(value)
                );
                data.setTxValue(
                    jsonHexToU256(value)
                );
            }
            if (this.data != null && !this.data.isEmpty()) {
                data.setData(
                    jsonHexToHexBytes(this.data)
                );
            }
            if (nonce != null && !nonce.isEmpty()) {
                data.setTxNonce(jsonHexToLong(nonce));
            }
            return data;
        }

        @Override
        public String toString() {
            return "CallArguments{" +
                "from='" + from + '\'' +
                ", to='" + to + '\'' +
                ", gas='" + gas + '\'' +
                ", gasPrice='" + gasPrice + '\'' +
                ", value='" + value + '\'' +
                ", data='" + data + '\'' +
                ", nonce='" + nonce + '\'' +
                '}';
        }
    }

    class BlockResult {
        public String number; // QUANTITY - the block number. null when its pending block.
        public String hash; // DATA, 32 Bytes - hash of the block. null when its pending block.
        public String parentHash; // DATA, 32 Bytes - hash of the parent block.
        public String nonce; // DATA, 8 Bytes - hash of the generated proof-of-work. null when its pending block.
        public String sha3Uncles; // DATA, 32 Bytes - SHA3 of the uncles data in the block.
        public String logsBloom; // DATA, 256 Bytes - the bloom filter for the logs of the block. null when its pending block.
        public String transactionsRoot; // DATA, 32 Bytes - the root of the transaction trie of the block.
        public String stateRoot; // DATA, 32 Bytes - the root of the final state trie of the block.
        public String receiptsRoot; // DATA, 32 Bytes - the root of the receipts trie of the block.
        public String miner; // DATA, 20 Bytes - the address of the beneficiary to whom the mining rewards were given.
        public String difficulty; // QUANTITY - integer of the difficulty for this block.
        public String totalDifficulty; // QUANTITY - integer of the total difficulty of the chain until this block.
        public String extraData; // DATA - the "extra data" field of this block
        public String size;//QUANTITY - integer the size of this block in bytes.
        public String gasLimit;//: QUANTITY - the maximum gas allowed in this block.
        public String gasUsed; // QUANTITY - the total used gas by all transactions in this block.
        public String timestamp; //: QUANTITY - the unix timestamp for when the block was collated.
        public Object[] transactions; //: Array - Array of transaction objects, or 32 Bytes transaction hashes depending on the last given parameter.
        public String[] uncles; //: Array - Array of uncle hashes.

        @Override
        public String toString() {
            return "BlockResult{" +
                "number='" + number + '\'' +
                ", hash='" + hash + '\'' +
                ", parentHash='" + parentHash + '\'' +
                ", nonce='" + nonce + '\'' +
                ", sha3Uncles='" + sha3Uncles + '\'' +
                ", logsBloom='" + logsBloom + '\'' +
                ", transactionsRoot='" + transactionsRoot + '\'' +
                ", stateRoot='" + stateRoot + '\'' +
                ", receiptsRoot='" + receiptsRoot + '\'' +
                ", miner='" + miner + '\'' +
                ", difficulty='" + difficulty + '\'' +
                ", totalDifficulty='" + totalDifficulty + '\'' +
                ", extraData='" + extraData + '\'' +
                ", size='" + size + '\'' +
                ", gas='" + gasLimit + '\'' +
                ", gasUsed='" + gasUsed + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", transactions=" + Arrays.toString(transactions) +
                ", uncles=" + Arrays.toString(uncles) +
                '}';
        }
    }

    class FilterRequest {
        public String fromBlock;
        public String toBlock;
        public Object address;
        public Object[] topics;
        public String blockHash;  // EIP-234: makes fromBlock = toBlock = blockHash

        @Override
        public String toString() {
            return "FilterRequest{" +
                "fromBlock='" + fromBlock + '\'' +
                ", toBlock='" + toBlock + '\'' +
                ", address=" + address +
                ", topics=" + Arrays.toString(topics) +
                ", blockHash='" + blockHash + '\'' +
                '}';
        }
    }

    class LogFilterElement {
        public String logIndex;
        public String transactionIndex;
        public String transactionHash;
        public String blockHash;
        public String blockNumber;
        public String address;
        public String data;
        public String[] topics;

        public LogFilterElement(LogInfo logInfo, Block b, Integer txIndex, Transaction tx, int logIdx) {
            logIndex = toJsonHex(logIdx);
            blockNumber = b == null ? null : toJsonHex(b.getHeight());
            blockHash = b == null ? null : toJsonHex(b.getHash().getBytes());
            transactionIndex = b == null ? null : toJsonHex(txIndex);
            transactionHash = toJsonHex(tx.getHash());
            address = toJsonHex(tx.getReceiveAddress());
            data = toJsonHex(logInfo.getData());
            topics = new String[logInfo.getTopics().size()];
            for (int i = 0; i < topics.length; i++) {
                topics[i] = toJsonHex(logInfo.getTopics().get(i).getData());
            }
        }

        @Override
        public String toString() {
            return "LogFilterElement{" +
                "logIndex='" + logIndex + '\'' +
                ", blockNumber='" + blockNumber + '\'' +
                ", blockHash='" + blockHash + '\'' +
                ", transactionHash='" + transactionHash + '\'' +
                ", transactionIndex='" + transactionIndex + '\'' +
                ", address='" + address + '\'' +
                ", data='" + data + '\'' +
                ", topics=" + Arrays.toString(topics) +
                '}';
        }
    }
}


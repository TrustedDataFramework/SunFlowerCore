package org.tdf.sunflower.types;

import org.spongycastle.util.BigIntegers;
import org.tdf.common.util.ByteUtil;
import org.tdf.rlp.RLPCodec;
import org.tdf.rlp.RLPElement;
import org.tdf.rlp.RLPItem;
import org.tdf.rlp.RLPList;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.apache.commons.lang3.ArrayUtils.nullToEmpty;
import static org.tdf.common.util.ByteUtil.EMPTY_BYTE_ARRAY;
import static org.tdf.common.util.ByteUtil.toHexString;

/**
 * The transaction receipt is a tuple of three items
 * comprising the transaction, together with the post-transaction state,
 * and the cumulative gas used in the block containing the transaction receipt
 * as of immediately after the transaction has happened,
 */
public class TransactionReceipt {

    private Transaction transaction;
    private byte[] postTxState = EMPTY_BYTE_ARRAY;
    private byte[] cumulativeGas = EMPTY_BYTE_ARRAY;
    private Bloom bloomFilter = new Bloom();
    private List<LogInfo> logInfoList = new ArrayList<>();
    private byte[] gasUsed = EMPTY_BYTE_ARRAY;
    private byte[] executionResult = EMPTY_BYTE_ARRAY;
    private String error = "";
    /* Tx Receipt in encoded form */
    private byte[] rlpEncoded;

    public TransactionReceipt() {
    }


    public TransactionReceipt(byte[] rlp) {

        RLPList params = RLPElement.fromEncoded(rlp).asRLPList();
        RLPList receipt = params.get(0).asRLPList();

        RLPItem postTxStateRLP = (RLPItem) receipt.get(0);
        RLPItem cumulativeGasRLP = (RLPItem) receipt.get(1);
        RLPItem bloomRLP = (RLPItem) receipt.get(2);
        RLPList logs = (RLPList) receipt.get(3);
        RLPItem gasUsedRLP = (RLPItem) receipt.get(4);
        RLPItem result = (RLPItem) receipt.get(5);

        postTxState = nullToEmpty(postTxStateRLP.asBytes());
        cumulativeGas = cumulativeGasRLP.asBytes();
        bloomFilter = new Bloom(bloomRLP.asBytes());
        gasUsed = gasUsedRLP.asBytes();
        executionResult = (executionResult = result.asBytes()) == null ? EMPTY_BYTE_ARRAY : executionResult;

        if (receipt.size() > 6) {
            byte[] errBytes = receipt.get(6).asBytes();
            error = errBytes != null ? new String(errBytes, StandardCharsets.UTF_8) : "";
        }

        for (RLPElement log : logs) {
            LogInfo logInfo = new LogInfo(log.asBytes());
            logInfoList.add(logInfo);
        }

        rlpEncoded = rlp;
    }

    public TransactionReceipt(byte[] postTxState, byte[] cumulativeGas,
                              Bloom bloomFilter, List<LogInfo> logInfoList) {
        this.postTxState = postTxState;
        this.cumulativeGas = cumulativeGas;
        this.bloomFilter = bloomFilter;
        this.logInfoList = logInfoList;
    }

    public TransactionReceipt(final RLPList rlpList) {
        if (rlpList == null || rlpList.size() != 4)
            throw new RuntimeException("Should provide RLPList with postTxState, cumulativeGas, bloomFilter, logInfoList");

        this.postTxState = rlpList.get(0).asBytes();
        this.cumulativeGas = rlpList.get(1).asBytes();
        this.bloomFilter = new Bloom(rlpList.get(2).asBytes());

        List<LogInfo> logInfos = new ArrayList<>();
        for (RLPElement logInfoEl : (RLPList) rlpList.get(3)) {
            LogInfo logInfo = new LogInfo(logInfoEl.asBytes());
            logInfos.add(logInfo);
        }
        this.logInfoList = logInfos;
    }

    public byte[] getPostTxState() {
        return postTxState;
    }

    public void setPostTxState(byte[] postTxState) {
        this.postTxState = postTxState;
        rlpEncoded = null;
    }

    public byte[] getCumulativeGas() {
        return cumulativeGas;
    }

    public void setCumulativeGas(long cumulativeGas) {
        this.cumulativeGas = BigIntegers.asUnsignedByteArray(BigInteger.valueOf(cumulativeGas));
        rlpEncoded = null;
    }

    public void setCumulativeGas(byte[] cumulativeGas) {
        this.cumulativeGas = cumulativeGas;
        rlpEncoded = null;
    }

    public byte[] getGasUsed() {
        return gasUsed;
    }

    public void setGasUsed(byte[] gasUsed) {
        this.gasUsed = gasUsed;
        rlpEncoded = null;
    }

    public void setGasUsed(long gasUsed) {
        this.gasUsed = BigIntegers.asUnsignedByteArray(BigInteger.valueOf(gasUsed));
        rlpEncoded = null;
    }

    public byte[] getExecutionResult() {
        return executionResult;
    }

    public void setExecutionResult(byte[] executionResult) {
        this.executionResult = executionResult;
        rlpEncoded = null;
    }

    public long getCumulativeGasLong() {
        return new BigInteger(1, cumulativeGas).longValue();
    }

    public Bloom getBloomFilter() {
        return bloomFilter;
    }

    public List<LogInfo> getLogInfoList() {
        return logInfoList;
    }

    public void setLogInfoList(List<LogInfo> logInfoList) {
        if (logInfoList == null) return;
        this.logInfoList = logInfoList;

        for (LogInfo loginfo : logInfoList) {
            bloomFilter.or(loginfo.getBloom());
        }
        rlpEncoded = null;
    }

    public boolean isValid() {
        return ByteUtil.byteArrayToLong(gasUsed) > 0;
    }

    public boolean isSuccessful() {
        return error.isEmpty();
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error == null ? "" : error;
    }

    /**
     * Used for Receipt trie hash calculation. Should contain only the following items encoded:
     * [postTxState, cumulativeGas, bloomFilter, logInfoList]
     */
    public byte[] getReceiptTrieEncoded() {
        return getEncoded(true);
    }

    /**
     * Used for serialization, contains all the receipt data encoded
     */
    public byte[] getEncoded() {
        if (rlpEncoded == null) {
            rlpEncoded = getEncoded(false);
        }

        return rlpEncoded;
    }

    public byte[] getEncoded(boolean receiptTrie) {

        byte[] postTxStateRLP = RLPCodec.encodeBytes(this.postTxState);
        byte[] cumulativeGasRLP = RLPCodec.encodeBytes(this.cumulativeGas);
        byte[] bloomRLP = RLPCodec.encodeBytes(this.bloomFilter.getData());

        final byte[] logInfoListRLP;
        if (logInfoList != null) {
            byte[][] logInfoListE = new byte[logInfoList.size()][];

            int i = 0;
            for (LogInfo logInfo : logInfoList) {
                logInfoListE[i] = logInfo.getEncoded();
                ++i;
            }
            logInfoListRLP = RLPCodec.encodeElements(Arrays.asList(logInfoListE));
        } else {
            logInfoListRLP = RLPCodec.encodeElements(Collections.emptyList());
        }

        return receiptTrie ?
                RLPCodec.encodeElements(Arrays.asList(postTxStateRLP, cumulativeGasRLP, bloomRLP, logInfoListRLP)) :
                RLPCodec.encodeElements(
                        Arrays.asList(
                        postTxStateRLP, cumulativeGasRLP, bloomRLP, logInfoListRLP,
                        RLPCodec.encodeBytes(gasUsed), RLPCodec.encodeBytes(executionResult),
                        RLPCodec.encodeBytes(error.getBytes(StandardCharsets.UTF_8))
                        )
                );

    }

    public void setTxStatus(boolean success) {
        this.postTxState = success ? new byte[]{1} : new byte[0];
        rlpEncoded = null;
    }

    public boolean hasTxStatus() {
        return postTxState != null && postTxState.length <= 1;
    }

    public boolean isTxStatusOK() {
        return postTxState != null && postTxState.length == 1 && postTxState[0] == 1;
    }

    public Transaction getTransaction() {
        if (transaction == null)
            throw new NullPointerException("Transaction is not initialized. Use TransactionInfo and BlockStore to setup Transaction instance");
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    @Override
    public String toString() {

        // todo: fix that

        return "TransactionReceipt[" +
                "\n  , " + (hasTxStatus() ? ("txStatus=" + (isTxStatusOK() ? "OK" : "FAILED"))
                : ("postTxState=" + toHexString(postTxState))) +
                "\n  , cumulativeGas=" + toHexString(cumulativeGas) +
                "\n  , gasUsed=" + toHexString(gasUsed) +
                "\n  , error=" + error +
                "\n  , executionResult=" + toHexString(executionResult) +
                "\n  , bloom=" + bloomFilter.toString() +
                "\n  , logs=" + logInfoList +
                ']';
    }

}
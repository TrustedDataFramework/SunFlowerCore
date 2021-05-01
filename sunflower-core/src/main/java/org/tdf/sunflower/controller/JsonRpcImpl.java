package org.tdf.sunflower.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.tdf.common.types.Uint256;
import org.tdf.common.util.HashUtil;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.facade.SunflowerRepository;
import org.tdf.sunflower.state.AccountTrie;
import org.tdf.sunflower.state.Address;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.vm.Backend;
import org.tdf.sunflower.vm.CallData;
import org.tdf.sunflower.vm.VMExecutor;
import org.tdf.sunflower.vm.hosts.Limit;

import java.math.BigInteger;
import java.util.List;
import java.util.Objects;

import static org.tdf.sunflower.controller.TypeConverter.*;

@Service
@RequiredArgsConstructor
public class JsonRpcImpl implements JsonRpc{
    private final AccountTrie accountTrie;
    private final SunflowerRepository repository;

    @Override
    public String web3_clientVersion() {
        return "tdos";
    }

    @Override
    public String web3_sha3(String data) throws Exception {
        byte[] result = HashUtil.sha3(hexToByteArray(data));
        return toJsonHex(result);
    }

    @Override
    public String net_version() {
        return Integer.toString(
                102
        );
    }

    @Override
    public String net_peerCount() {
        return TypeConverter.toJsonHex(0);
    }

    @Override
    public boolean net_listening() {
        return false;
    }

    @Override
    public String eth_protocolVersion() {
        return "farmbase";
    }

    @Override
    public Object eth_syncing() {
        return false;
    }

    @Override
    public String eth_coinbase() {
        return toJsonHex(Address.empty().getBytes());
    }

    @Override
    public boolean eth_mining() {
        return false;
    }

    @Override
    public String eth_hashrate() {
        return toJsonHex(BigInteger.ZERO);
    }

    @Override
    public String eth_gasPrice() {
        return toJsonHex(0);
    }

    @Override
    public String[] eth_accounts() {
        return new String[0];
    }

    @Override
    public String eth_blockNumber() {
        return toJsonHex(repository.getBestHeader().getHeight());
    }


    private Backend getBackendByBlockId(String blockId, boolean isStatic) {
        Header header;
        if (blockId == null || blockId.trim().isEmpty())
            blockId = "latest";

        switch (blockId) {
            case "latest":
            case "pending":
                header = repository.getBestHeader();
                break;
            case "earliest":
                header = repository.getGenesis().getHeader();
                break;
            default:
                int h = hexToBigInteger(blockId).intValue();
                header = repository.getCanonicalHeader(h).get();
                break;
        }
        return accountTrie.createBackend(header, System.currentTimeMillis() / 1000, isStatic);
    }

    @Override
    public String eth_getBalance(String address, String block) throws Exception {
        Objects.requireNonNull(address, "address is required");
        Backend repo = getBackendByBlockId(block, true);

        byte[] addressAsByteArray = hexToByteArray(address);
        Uint256 balance = repo.getBalance(HexBytes.fromBytes(addressAsByteArray));
        return toJsonHex(balance.value());
    }

    @Override
    public String eth_getLastBalance(String address) throws Exception {
        return eth_getBalance(address, "latest");
    }

    @Override
    public String eth_getStorageAt(String address, String storageIdx, String blockId) throws Exception {
        return null;
    }

    @Override
    public String eth_getTransactionCount(String address, String blockId) throws Exception {
        return null;
    }

    @Override
    public String eth_getBlockTransactionCountByHash(String blockHash) throws Exception {
        return null;
    }

    @Override
    public String eth_getBlockTransactionCountByNumber(String bnOrId) throws Exception {
        return null;
    }

    @Override
    public String eth_getUncleCountByBlockHash(String blockHash) throws Exception {
        return null;
    }

    @Override
    public String eth_getUncleCountByBlockNumber(String bnOrId) throws Exception {
        return null;
    }

    @Override
    public String eth_getCode(String addr, String bnOrId) throws Exception {
        return null;
    }

    @Override
    public String eth_sign(String addr, String data) throws Exception {
        return null;
    }

    @Override
    public String eth_sendTransaction(CallArguments transactionArgs) throws Exception {
        return null;
    }

    @Override
    public String eth_sendRawTransaction(String rawData) throws Exception {
        return null;
    }

    @Override
    public String eth_call(CallArguments args, String bnOrId) throws Exception {
        CallData callData = Objects.requireNonNull(args).toCallData();
        Backend backend = getBackendByBlockId(bnOrId, true);
        VMExecutor executor = new VMExecutor(backend, callData, new Limit(), 0);

        return toJsonHex(executor.execute().getReturns());
    }

    @Override
    public String eth_estimateGas(CallArguments args) throws Exception {
        return null;
    }

    @Override
    public BlockResult eth_getBlockByHash(String blockHash, Boolean fullTransactionObjects) throws Exception {
        return null;
    }

    @Override
    public BlockResult eth_getBlockByNumber(String bnOrId, Boolean fullTransactionObjects) throws Exception {
        return null;
    }

    @Override
    public TransactionResultDTO eth_getTransactionByHash(String transactionHash) throws Exception {
        return null;
    }

    @Override
    public TransactionResultDTO eth_getTransactionByBlockHashAndIndex(String blockHash, String index) throws Exception {
        return null;
    }

    @Override
    public TransactionResultDTO eth_getTransactionByBlockNumberAndIndex(String bnOrId, String index) throws Exception {
        return null;
    }

    @Override
    public TransactionReceiptDTO eth_getTransactionReceipt(String transactionHash) throws Exception {
        return null;
    }

    @Override
    public TransactionReceiptDTOExt ethj_getTransactionReceipt(String transactionHash) throws Exception {
        return null;
    }

    @Override
    public BlockResult eth_getUncleByBlockHashAndIndex(String blockHash, String uncleIdx) throws Exception {
        return null;
    }

    @Override
    public BlockResult eth_getUncleByBlockNumberAndIndex(String blockId, String uncleIdx) throws Exception {
        return null;
    }

    @Override
    public String eth_newFilter(FilterRequest fr) throws Exception {
        return null;
    }

    @Override
    public String eth_newBlockFilter() {
        return null;
    }

    @Override
    public String eth_newPendingTransactionFilter() {
        return null;
    }

    @Override
    public boolean eth_uninstallFilter(String id) {
        return false;
    }

    @Override
    public Object[] eth_getFilterChanges(String id) {
        return new Object[0];
    }

    @Override
    public Object[] eth_getFilterLogs(String id) {
        return new Object[0];
    }

    @Override
    public Object[] eth_getLogs(FilterRequest fr) throws Exception {
        return new Object[0];
    }

    @Override
    public List<Object> eth_getWork() {
        return null;
    }

    @Override
    public boolean eth_submitWork(String nonce, String header, String digest) throws Exception {
        return false;
    }

    @Override
    public boolean eth_submitHashrate(String hashrate, String id) {
        return false;
    }

    @Override
    public String personal_signAndSendTransaction(CallArguments tx, String password) {
        return null;
    }
}

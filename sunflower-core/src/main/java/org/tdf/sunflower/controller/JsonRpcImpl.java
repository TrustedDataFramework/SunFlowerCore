package org.tdf.sunflower.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.tdf.common.types.Uint256;
import org.tdf.common.util.HashUtil;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.AppConfig;
import org.tdf.sunflower.facade.ConsensusEngine;
import org.tdf.sunflower.facade.IRepositoryService;
import org.tdf.sunflower.facade.RepositoryReader;
import org.tdf.sunflower.facade.TransactionPool;
import org.tdf.sunflower.state.AccountTrie;
import org.tdf.sunflower.state.Address;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.types.Transaction;
import org.tdf.sunflower.types.TransactionInfo;
import org.tdf.sunflower.vm.Backend;
import org.tdf.sunflower.vm.CallData;
import org.tdf.sunflower.vm.VMExecutor;

import java.math.BigInteger;
import java.util.*;

import static org.tdf.sunflower.controller.TypeConverter.*;

@Service
@RequiredArgsConstructor
public class JsonRpcImpl implements JsonRpc {
    private final AccountTrie accountTrie;
    private final IRepositoryService repository;
    private final TransactionPool pool;
    private final ConsensusEngine engine;

    private Block getByJsonBlockId(String id) {
        try (RepositoryReader rd = repository.getReader()) {
            if ("earliest".equalsIgnoreCase(id)) {
                return rd.getGenesis();
            } else if ("latest".equalsIgnoreCase(id)) {
                return rd.getBestBlock();
            } else if ("pending".equalsIgnoreCase(id)) {
                return null;
            } else {
                long blockNumber = hexToBigInteger(id).longValue();
                return rd.getCanonicalBlock(blockNumber);
            }
        }

    }

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
            engine.getChainId()
        );
    }

    @Override
    public String eth_chainId() {
        return toJsonHex(engine.getChainId());
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
        try (RepositoryReader rd = repository.getReader()) {
            return toJsonHex(rd.getBestHeader().getHeight());
        }
    }


    private Backend getBackendByBlockId(String blockId, boolean isStatic) {
        Header header;
        if (blockId == null || blockId.trim().isEmpty())
            blockId = "latest";

        try (RepositoryReader rd = repository.getReader()) {

            switch (blockId) {
                case "latest": {
                    header = rd.getBestHeader();
                    break;
                }
                case "pending":
                    return pool.current();
                case "earliest":
                    header = rd.getGenesis().getHeader();
                    break;
                default:
                    int h = hexToBigInteger(blockId).intValue();
                    header = rd.getCanonicalHeader(h);
                    break;
            }
            return accountTrie.createBackend(header, System.currentTimeMillis() / 1000, isStatic);
        }

    }

    @Override
    public String eth_getBalance(String address, String block) throws Exception {
        Objects.requireNonNull(address, "address is required");

        try (Backend repo = getBackendByBlockId(block, true)) {
            byte[] addressAsByteArray = hexToByteArray(address);
            Uint256 balance = repo.getBalance(HexBytes.fromBytes(addressAsByteArray));
            return toJsonHex(balance.getValue());
        }
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
        try (
            Backend backend = getBackendByBlockId(blockId, true)
        ) {
            long n = backend.getNonce(jsonHexToHexBytes(address));
            return toJsonHex(n);
        }
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
        return toJsonHex(0);
    }

    @Override
    public String eth_getUncleCountByBlockNumber(String bnOrId) throws Exception {
        return toJsonHex(0);
    }

    @Override
    public String eth_getCode(String addr, String bnOrId) throws Exception {
        try (Backend backend = getBackendByBlockId(bnOrId, true)) {
            HexBytes code = backend.getCode(jsonHexToHexBytes(addr));
            return toJsonHex(code);
        }
    }

    @Override
    public String eth_sign(String addr, String data) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public String eth_sendTransaction(CallArguments transactionArgs) throws Exception {
        // for security issues, eth_sendTransaction is not disabled
        throw new UnsupportedOperationException();
    }

    @Override
    public String eth_sendRawTransaction(String rawData) throws Exception {
        Transaction tx = new Transaction(hexToByteArray(rawData));
        Map<HexBytes, String> errors = pool.collect(tx);
        if (errors.get(tx.getHashHex()) != null)
            throw new RuntimeException(errors.get(tx.getHashHex()));
        return TypeConverter.toJsonHex(tx.getHash());
    }

    @Override
    public String eth_call(CallArguments args, String bnOrId) throws Exception {
        long start = System.currentTimeMillis();
        CallData callData = Objects.requireNonNull(args).toCallData();
        try (
            Backend backend = getBackendByBlockId(bnOrId, true)
        ) {
            VMExecutor executor = new VMExecutor(backend, callData, AppConfig.INSTANCE.getBlockGasLimit());
            return toJsonHex(executor.execute().getExecutionResult());
        } finally {
            System.out.println("eth call use " + (System.currentTimeMillis() - start) + " ms");
        }
    }

    @Override
    public String eth_estimateGas(CallArguments args) throws Exception {
        CallData callData = Objects.requireNonNull(args).toCallData();
        try (Backend backend = getBackendByBlockId("latest", true)) {
            VMExecutor executor = new VMExecutor(backend, callData, AppConfig.INSTANCE.getBlockGasLimit());
            return toJsonHex(executor.execute().getGasUsed());
        }
    }

    @Override
    public BlockResult eth_getBlockByHash(String blockHash, Boolean fullTransactionObjects) throws Exception {
        Block b = getByJsonBlockId(blockHash);
        return getBlockResult(b, fullTransactionObjects);
    }

    @Override
    public BlockResult eth_getBlockByNumber(String bnOrId, Boolean fullTransactionObjects) throws Exception {
        Block b = getByJsonBlockId(bnOrId);
        return getBlockResult(b, fullTransactionObjects);
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
        HexBytes hash = jsonHexToHexBytes(transactionHash);
        try (RepositoryReader rd = repository.getReader()) {
            TransactionInfo info = rd.getTransactionInfo(hash);
            Transaction tx = info == null ? null : info.getReceipt().getTransaction();
            Block b = info == null ? null : rd.getBlockByHash(HexBytes.fromBytes(info.getBlockHash()));
            if (info == null || tx == null || b == null)
                return null;
            info.setTransaction(tx);
            return new TransactionReceiptDTO(b, info);
        }

    }

    @Override
    public TransactionReceiptDTOExt ethj_getTransactionReceipt(String transactionHash) throws Exception {
        throw new UnsupportedOperationException();
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

    protected BlockResult getBlockResult(Block block, boolean fullTx) {
        if (block == null)
            return null;
        BlockResult br = new BlockResult();
        boolean isPending = false;

        br.number = isPending ? null : toJsonHex(block.getHeight());
        br.hash = isPending ? null : toJsonHex(block.getHash());
        br.parentHash = toJsonHex(block.getHashPrev());
        br.nonce = isPending ? null : toJsonHex(block.getNonce());
        br.sha3Uncles = toJsonHex(block.getUnclesHash());
        br.logsBloom = isPending ? null : toJsonHex(block.getLogsBloom());
        br.transactionsRoot = toJsonHex(block.getTransactionsRoot());
        br.stateRoot = toJsonHex(block.getStateRoot());
        br.receiptsRoot = toJsonHex(block.getReceiptTrieRoot());
        br.miner = isPending ? null : toJsonHex(block.getCoinbase());
        br.difficulty = toJsonHex(new BigInteger(1, block.getDifficulty().getBytes()));
        br.totalDifficulty = toJsonHex(BigInteger.ZERO);
        if (block.getExtraData() != null)
            br.extraData = toJsonHex(block.getExtraData());
        // TODO: estimate size
        br.size = null;
        br.gasLimit = toJsonHex(block.getGasLimit());
        br.gasUsed = toJsonHex(block.getGasUsed());
        br.timestamp = toJsonHex(block.getCreatedAt());

        List<Object> txes = new ArrayList<>();
        if (fullTx) {
            for (int i = 0; i < block.getBody().size(); i++) {
                txes.add(new TransactionResultDTO(block, i, block.getBody().get(i)));
            }
        } else {
            for (Transaction tx : block.getBody()) {
                txes.add(toJsonHex(tx.getHash()));
            }
        }
        br.transactions = txes.toArray();

        List<String> ul = Collections.emptyList();
        br.uncles = ul.toArray(new String[0]);

        return br;
    }
}

package org.tdf.sunflower.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sun.management.OperatingSystemMXBean;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.tdf.common.store.Store;
import org.tdf.common.trie.Trie;
import org.tdf.common.types.BlockConfirms;
import org.tdf.common.types.Uint256;
import org.tdf.common.util.HexBytes;
import org.tdf.common.util.IntSerializer;
import org.tdf.rlp.RLPCodec;
import org.tdf.rlp.RLPList;
import org.tdf.sunflower.GlobalConfig;
import org.tdf.sunflower.Start;
import org.tdf.sunflower.consensus.pow.PoW;
import org.tdf.sunflower.consensus.vrf.contract.VrfPreBuiltContract;
import org.tdf.sunflower.consensus.vrf.util.VrfUtil;
import org.tdf.sunflower.facade.ConsensusEngine;
import org.tdf.sunflower.facade.Miner;
import org.tdf.sunflower.facade.SunflowerRepository;
import org.tdf.sunflower.facade.TransactionPool;
import org.tdf.sunflower.net.Peer;
import org.tdf.sunflower.net.PeerServer;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.AccountTrie;
import org.tdf.sunflower.state.Address;
import org.tdf.sunflower.sync.SyncManager;
import org.tdf.sunflower.types.*;
import org.tdf.sunflower.util.EnvReader;
import org.tdf.sunflower.util.FileUtils;
import org.tdf.sunflower.util.MappingUtil;
import org.tdf.sunflower.vm.abi.ContractABI;
import org.tdf.sunflower.vm.abi.ContractCallPayload;

import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.tdf.sunflower.state.Constants.VRF_BIOS_CONTRACT_ADDR;

@RestController
@AllArgsConstructor
@RequestMapping("/rpc")
@Slf4j(topic = "rpc")
public class EntryController {
    private AccountTrie accountTrie;

    @Qualifier("contractStorageTrie")
    private Trie<byte[], byte[]> contractStorageTrie;

    private GlobalConfig config;

    private PeerServer peerServer;

    private TransactionPool pool;

    private SunflowerRepository sunflowerRepository;

    private ObjectMapper objectMapper;

    private SunflowerRepository repository;

    private SyncManager syncManager;

    private ConsensusEngine consensusEngine;

    private Miner miner;

    private ApplicationContext ctx;

    @SneakyThrows
    private Stat createState() {
        Stat.StatBuilder builder = Stat.builder();
        OperatingSystemMXBean osMxBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        Block best = repository.getBestBlock();
        List<Block> blocks = repository.getBlocksBetween(Math.max(0, best.getHeight() - 10), best.getHeight());

        Uint256 totalGasPrice = Uint256.ZERO;
        long totalTransactions = 0;
        for (Block b : blocks) {
            for (Transaction t : b.getBody()) {
                if (t.getType() == Transaction.Type.COIN_BASE.code)
                    continue;
                totalGasPrice = totalGasPrice.safeAdd(t.getGasPrice());
                totalTransactions++;
            }
        }

        long yesterday = System.currentTimeMillis() / 1000 - (24 * 60 * 60);
        Optional<Header> o = binarySearch(yesterday, 0, best.getHeight());

        String diff = consensusEngine.getName().equals("pow")
                ? HexBytes.encode(((PoW) consensusEngine).getNBits(best.getStateRoot().getBytes()))
                : "";

        EnvReader rd = new EnvReader(ctx.getEnvironment());
        return builder
                .cpu(osMxBean.getSystemLoadAverage())
                .memoryUsed(osMxBean.getTotalPhysicalMemorySize() - osMxBean.getFreePhysicalMemorySize())
                .totalMemory(osMxBean.getTotalPhysicalMemorySize())
                .averageGasPrice(
                        totalTransactions == 0 ? Uint256.ZERO : totalGasPrice.div(Uint256.of(totalTransactions)))
                .averageBlockInterval(rd.getBlockInterval()).height(best.getHeight())
                .mining(blocks.stream().anyMatch(
                        x -> x.getBody().size() > 0 && x.getBody().get(0).getTo().equals(miner.getMinerAddress())))
                .currentDifficulty(diff).transactionPoolSize(pool.size())
                .blocksPerDay(o.map(h -> best.getHeight() - h.getHeight() + 1).orElse(0L))
                .consensus(consensusEngine.getName())
                .genesis(rd.getGenesis())
                .ec(rd.getEC())
                .hash(rd.getHash())
                .ae(rd.getAE())
                .blockInterval(rd.getBlockInterval())
                .p2pAddress(peerServer.getSelf().encodeURI())
                .blocksPerEra(rd.getBlocksPerEra())
                .maxMiners(rd.getMaxMiners())
                .allowUnauthorized(rd.isAllowUnauthorized())
                .build()
                ;
    }

    private <T> T getBlockOrHeader(String hashOrHeight, Function<Long, Optional<T>> func,
                                   Function<byte[], Optional<T>> func1) {
        Long height = null;
        try {
            height = Long.parseLong(hashOrHeight);
            Header h = repository.getBestHeader();
            while (height < 0) {
                height += h.getHeight() + 1;
            }
        } catch (Exception ignored) {

        }
        if (height != null) {
            final long finalHeight = height;
            return func.apply(height)
                    .orElseThrow(() -> new RuntimeException("block at height " + finalHeight + " not found"));
        }
        return func1.apply(HexBytes.decode(hashOrHeight))
                .orElseThrow(() -> new RuntimeException("block of hash " + hashOrHeight + " not found"));
    }

    @GetMapping(value = "/block/{hashOrHeight}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Block getBlock(@PathVariable String hashOrHeight) {
        return getBlockOrHeader(hashOrHeight, repository::getCanonicalBlock, repository::getBlock);
    }

    @GetMapping(value = "/header/{hashOrHeight}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Header getHeaders(@PathVariable String hashOrHeight) {
        return getBlockOrHeader(hashOrHeight, repository::getCanonicalHeader, repository::getHeader);
    }

    @GetMapping(value = "/transaction/{hash}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> getTransaction(@PathVariable String hash) throws Exception {
        Optional<Transaction> o = repository.getTransactionByHash(HexBytes.decode(hash));
        if (o.isPresent()) {
            Map<String, Object> m = MappingUtil.pojoToMap(o.get());
            BlockConfirms confirms = repository.getConfirms(HexBytes.decode(hash));
            m.put("confirms", confirms.getConfirms());
            m.put("blockHeight", confirms.getBlockHeight());
            m.put("blockHash", confirms.getBlockHash().toHex());
            return m;
        }
        o = pool.get(HexBytes.fromHex(hash));
        if (o.isPresent()) {
            Map<String, Object> m = MappingUtil.pojoToMap(o.get());
            m.put("confirms", -1);
            return m;
        }
        throw new RuntimeException("transaction hash " + hash + " not found");
    }

    @GetMapping(value = "/account/{addressOrPublicKey}", produces = MediaType.APPLICATION_JSON_VALUE)
    public AccountView getAccount(@PathVariable String addressOrPublicKey) throws Exception {
        HexBytes addressHex = Address.of(addressOrPublicKey);
        return accountTrie.get(sunflowerRepository.getBestHeader().getStateRoot().getBytes(), addressHex)
                .map(AccountView::fromAccount)
                .orElse(new AccountView(addressHex, 0, Uint256.ZERO, HexBytes.EMPTY, HexBytes.EMPTY, HexBytes.empty()));
    }

    @GetMapping(value = "/approved")
    public List<HexBytes> getApproved() {
        return consensusEngine.getApprovedNodes().map(ArrayList::new).orElse(null);
    }

    // TODO: enclose this config
    @GetMapping(value = "/config", produces = MediaType.APPLICATION_JSON_VALUE)
    public GlobalConfig config() {
        return config;
    }

    @GetMapping(value = "/peers", produces = MediaType.APPLICATION_JSON_VALUE)
    public PeersInfo peers() {
        return new PeersInfo(peerServer.getPeers(), peerServer.getBootStraps());
    }

    @GetMapping(value = "/miners", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<HexBytes> miners() {
        return consensusEngine.getMinerAddresses();
    }

    @GetMapping(value = "/orphan", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Block> orphans() {
        return syncManager.getOrphans();
    }

    @GetMapping(value = "/pool", produces = MediaType.APPLICATION_JSON_VALUE)
    public PagedView<Transaction> getPool(@ModelAttribute PoolQuery poolQuery) {
        switch (poolQuery.getStatus()) {
            case "pending":
                return pool.get(poolQuery);
            case "dropped":
                return pool.getDropped(poolQuery);
            default:
                throw new RuntimeException("unknown status " + poolQuery.getStatus());
        }
    }

    @PostMapping(value = "/transaction", produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<List<?>> sendTransaction(@RequestBody JsonNode node) {
        List<Transaction> ts;
        if (node.isArray()) {
            ts = Arrays.asList(objectMapper.convertValue(node, Transaction[].class));
        } else {
            ts = Collections.singletonList(objectMapper.convertValue(node, Transaction.class));
        }
        List<String> errors = pool.collect(ts);
        Response<List<?>> errResp = Response.newFailed(Response.Code.INTERNAL_ERROR, String.join("\n", errors));
        if (!errors.isEmpty())
            return errResp;

        return Response.newSuccessFul(ts.stream().map(Transaction::getHash).collect(Collectors.toList()));
    }

    @GetMapping(value = "/contract/{address}", produces = MediaType.APPLICATION_JSON_VALUE)
    public HexBytes getContract(@PathVariable("address") final String address,
                                @RequestParam(value = "parameters", required = false) String arguments,
                                @RequestParam(value = "args", required = false) String argsStr) {
        HexBytes addressHex = Address.of(address);
        arguments = arguments == null ? argsStr : arguments;
        if (arguments == null || arguments.isEmpty())
            throw new RuntimeException("require parameters or args");
        HexBytes args = HexBytes.fromHex(arguments);
        Header h = sunflowerRepository.getBestHeader();

        ContractCallPayload callPayload = RLPCodec.decode(args.getBytes(), ContractCallPayload.class);
        RLPList result = accountTrie.fork(h.getStateRoot().getBytes()).call(addressHex, callPayload.getMethod(),
                callPayload.getParameters());

        return HexBytes.fromBytes(result.getEncoded());
    }

    @PostMapping(value = "/contract/{address}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Object rpcQuery(@PathVariable("address") final String address,
                           @RequestBody(required = false) JsonNode body) {
        Object o = consensusEngine.rpcQuery(HexBytes.fromHex(address), body);
        if (o == null)
            return Response.newSuccessFul(null);
        return o;
    }

    @GetMapping(value = "/contract/{address}/abi", produces = MediaType.APPLICATION_JSON_VALUE)
    public Object getABI(@PathVariable("address") final String address) {
        HexBytes addressHex = Address.of(address);
        Header h = sunflowerRepository.getBestHeader();
        Account a = accountTrie.get(h.getStateRoot().getBytes(), addressHex).get();
        Store<byte[], byte[]> store = contractStorageTrie.revert(a.getStorageRoot());
        byte[] abiEncoded = store.get("__abi".getBytes(StandardCharsets.UTF_8)).get();
        return Arrays.stream(RLPCodec.decode(abiEncoded, ContractABI[].class)).map(ContractABI::toJSON)
                .collect(Collectors.toList());
    }

    @GetMapping(value = "/contract/vrf/{address}", produces = MediaType.APPLICATION_JSON_VALUE)
    public HexBytes getVrfContract(@PathVariable("address") final String address, String depAddr) throws Exception {
        HexBytes contractAddressHex = Address.of(address);
        Header h = sunflowerRepository.getBestHeader();
        if (VRF_BIOS_CONTRACT_ADDR.equals(address)) {
            if (depAddr != null && !depAddr.equals("")) {
                HexBytes depositAddress = HexBytes.fromHex(depAddr);
                return HexBytes.fromBytes(VrfUtil.getFromContractStorage(contractAddressHex, h,
                        depositAddress.getBytes(), accountTrie, contractStorageTrie));
            } else {
                return HexBytes.fromBytes(VrfUtil.getFromContractStorage(contractAddressHex, h,
                        VrfPreBuiltContract.TOTAL_KEY, accountTrie, contractStorageTrie));
            }

        }
        return HexBytes.fromBytes("NOT_VRF_CONTRACT_ADDRESS".getBytes());
    }

    @PostMapping(value = "/operations", produces = MediaType.APPLICATION_JSON_VALUE)
    public Object operations(@RequestBody JsonNode node) {
        String m = node.get("method").asText();
        switch (m) {
            case "export": {
                List<Transaction> li = new ArrayList<>();
                String publicKey = node.get("publicKey").asText();
                HexBytes pk = HexBytes.fromHex(publicKey);
                HexBytes address = Address.fromPublicKey(pk);
                sunflowerRepository.traverseTransactions((h, t) -> {
                    if (t.getFrom().equals(pk) || t.getTo().equals(address)) {
                        li.add(t);
                    }
                    return true;
                });
                return li;
            }
        }
        throw new RuntimeException("invalid payload " + node.toString());
    }

    @GetMapping(value = "/stat", produces = MediaType.APPLICATION_JSON_VALUE)
    public Stat stat() {
        return createState();
    }

    // get the nearest block after or equals to the timestamp
    private Optional<Header> binarySearch(long timestamp, long low, long high) {
        Header x = repository.getCanonicalHeader(low).get();
        if (low == high) {
            if (x.getCreatedAt() < timestamp)
                return Optional.empty();
            return Optional.of(x);
        }
        Header m = repository.getCanonicalHeader((low + high) / 2).get();
        if (m.getCreatedAt() == timestamp)
            return Optional.of(m);
        if (m.getCreatedAt() < timestamp) {
            if (m.getHeight() == high)
                return Optional.empty();

            Header m1 = repository.getCanonicalHeader(m.getHeight() + 1).get();
            if (m1.getCreatedAt() >= timestamp)
                return Optional.of(m1);
            return binarySearch(timestamp, Math.min(m.getHeight() + 1, high), high);
        }
        if (m.getHeight() == low) {
            return Optional.of(m);
        }
        Header m1 = repository.getCanonicalHeader(m.getHeight() - 1).get();
        if (m1.getCreatedAt() < timestamp)
            return Optional.of(m);
        if (m1.getCreatedAt() == timestamp)
            return Optional.of(m1);
        return binarySearch(timestamp, low, m.getHeight() - 1);
    }

    @AllArgsConstructor
    @Getter
    static class PeersInfo {
        List<Peer> peers;
        List<Peer> bootstraps;
    }

    @AllArgsConstructor
    @Getter
    static class AccountView {
        private HexBytes address;

        // for normal account this field is continuous integer
        // for contract account this field is nonce of deploy transaction
        @JsonSerialize(using = IntSerializer.class)
        private long nonce;

        // the balance of account
        // for contract account, this field is zero
        @JsonSerialize(using = IntSerializer.class)
        private Uint256 balance;

        // for normal address this field is null
        // for contract address this field is creator of this contract
        private HexBytes createdBy;

        // hash code of contract code
        // if the account contains none contract, contract hash will be null
        private HexBytes contractHash;

        // root hash of contract db
        // if the account is not contract account, this field will be null
        private HexBytes storageRoot;

        static AccountView fromAccount(Account account) {
            return new AccountView(account.getAddress(), account.getNonce(), account.getBalance(),
                    account.getCreatedBy(), HexBytes.fromBytes(account.getContractHash()),
                    HexBytes.fromBytes(account.getStorageRoot()));
        }
    }
}

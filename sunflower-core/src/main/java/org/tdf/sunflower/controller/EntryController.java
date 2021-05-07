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
import org.tdf.common.trie.Trie;
import org.tdf.common.types.Uint256;
import org.tdf.common.util.HexBytes;
import org.tdf.common.util.IntSerializer;
import org.tdf.common.util.RLPUtil;
import org.tdf.sunflower.AppConfig;
import org.tdf.sunflower.GlobalConfig;
import org.tdf.sunflower.consensus.poa.PoA;
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

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@AllArgsConstructor
@RequestMapping("/rpc")
@Slf4j(topic = "rpc")
public class EntryController {
    private final AccountTrie accountTrie;

    @Qualifier("contractStorageTrie")
    private final Trie<HexBytes, HexBytes> contractStorageTrie;

    private final GlobalConfig config;

    private final PeerServer peerServer;

    private final TransactionPool pool;

    private final SunflowerRepository sunflowerRepository;

    private final ObjectMapper objectMapper;

    private final SunflowerRepository repository;

    private final SyncManager syncManager;

    private final ConsensusEngine consensusEngine;

    private final Miner miner;

    private final ApplicationContext ctx;

    @SneakyThrows
    private Stat createState() {
        AppConfig c = AppConfig.get();

        Stat.StatBuilder builder = Stat.builder();
        OperatingSystemMXBean osMxBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        Block best = repository.getBestBlock();
        List<Block> blocks = repository.getBlocksBetween(Math.max(0, best.getHeight() - 10), best.getHeight());

        Uint256 totalGasPrice = Uint256.ZERO;
        long totalTransactions = 0;
        for (Block b : blocks) {
            for (int i = 0; i < b.getBody().size(); i++) {
                if (i == 0)
                    continue;
                totalGasPrice = totalGasPrice.plus(b.getBody().get(i).getGasPriceAsU256());
                totalTransactions++;
            }
        }

        long yesterday = System.currentTimeMillis() / 1000 - (24 * 60 * 60);
        Optional<Header> o = binarySearch(yesterday, 0, best.getHeight());

        String diff = "";

        EnvReader rd = new EnvReader(ctx.getEnvironment());
        return builder
            .cpu(osMxBean.getSystemLoadAverage())
            .memoryUsed(osMxBean.getTotalPhysicalMemorySize() - osMxBean.getFreePhysicalMemorySize())
            .totalMemory(osMxBean.getTotalPhysicalMemorySize())
            .averageGasPrice(
                totalTransactions == 0 ? Uint256.ZERO : totalGasPrice.div(Uint256.of(totalTransactions)))
            .averageBlockInterval(rd.getBlockInterval()).height(best.getHeight())
            .mining(blocks.stream().anyMatch(
                x -> x.getBody().size() > 0 && x.getBody().get(0).getReceiveHex().equals(miner.getMinerAddress())))
            .currentDifficulty(diff).transactionPoolSize(0)
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
            .gasPrice(c.getVmGasPrice().longValue())
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
    public BlockV1 getBlock(@PathVariable String hashOrHeight) {
        return BlockV1.fromV2(getBlockOrHeader(hashOrHeight, repository::getCanonicalBlock, repository::getBlock));
    }

    @GetMapping(value = "/header/{hashOrHeight}", produces = MediaType.APPLICATION_JSON_VALUE)
    public HeaderV1 getHeaders(@PathVariable String hashOrHeight) {
        return HeaderV1.fromV2(getBlockOrHeader(hashOrHeight, repository::getCanonicalHeader, repository::getHeader));
    }

    @GetMapping(value = "/transaction/{hash}", produces = MediaType.APPLICATION_JSON_VALUE)
    public TransactionV1 getTransaction(@PathVariable String hash) {
        return repository.getTransactionByHash(HexBytes.decode(hash)).map(TransactionV1::fromV2).get();
    }

    @GetMapping(value = "/account/{addressOrPublicKey}", produces = MediaType.APPLICATION_JSON_VALUE)
    public AccountView getAccount(@PathVariable String addressOrPublicKey) throws Exception {
        HexBytes addressHex = Address.of(addressOrPublicKey);
        Account a = accountTrie.get(sunflowerRepository.getBestHeader().getStateRoot(), addressHex);
        if (a == null)
            a = Account.emptyAccount(addressHex, Uint256.ZERO);
        return AccountView.fromAccount(a);
    }

    @GetMapping(value = "/peers", produces = MediaType.APPLICATION_JSON_VALUE)
    public PeersInfo peers() {
        return new PeersInfo(peerServer.getPeers(), peerServer.getBootStraps());
    }

    @GetMapping(value = "/miners", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<HexBytes> miners() {
        return Collections.emptyList();
    }

    @GetMapping(value = "/orphan", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Block> orphans() {
        return syncManager.getOrphans();
    }

    @GetMapping(value = "/pool", produces = MediaType.APPLICATION_JSON_VALUE)
    public PagedView<TransactionV1> getPool(@ModelAttribute PoolQuery poolQuery) {
        return PagedView.empty();
    }


    @GetMapping(value = "/contract/{address}/abi", produces = MediaType.APPLICATION_JSON_VALUE)
    public Object getABI(@PathVariable("address") final String address) {
        HexBytes addressHex = Address.of(address);
        Header h = sunflowerRepository.getBestHeader();
        Account a = accountTrie.get(h.getStateRoot(), addressHex);
        return null;
    }

    @PostMapping(value = "/operations", produces = MediaType.APPLICATION_JSON_VALUE)
    public Object operations(@RequestBody JsonNode node) {
        String m = node.get("method").asText();
        switch (m) {
            case "export": {
//                List<Transaction> li = new ArrayList<>();
//                String publicKey = node.get("publicKey").asText();
//                HexBytes pk = HexBytes.fromHex(publicKey);
//                HexBytes address = Address.fromPublicKey(pk);
//                sunflowerRepository.traverseTransactions((h, t) -> {
//                    if (t.getFrom().equals(pk) || t.getTo().equals(address)) {
//                        li.add(t);
//                    }
//                    return true;
//                });
                return new ArrayList<>();
            }
        }
        throw new RuntimeException("invalid payload " + node.toString());
    }

    @GetMapping(value = "/stat", produces = MediaType.APPLICATION_JSON_VALUE)
    public Stat stat() {
        return createState();
    }

    // if thread node not receive farm base transaciont and gateway node restarted
    // needs to construct the transaction
    @GetMapping(value = "/farmBaseTransactions", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<HexBytes> farmBaseTransactions() {
        PoA poa = (PoA) consensusEngine;
        return poa.farmBaseTransactions.stream().map(RLPUtil::encode).collect(Collectors.toList());
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
        private final HexBytes address;

        // for normal account this field is continuous integer
        // for contract account this field is nonce of deploy transaction
        @JsonSerialize(using = IntSerializer.class)
        private final long nonce;

        // the balance of account
        // for contract account, this field is zero
        @JsonSerialize(using = IntSerializer.class)
        private final Uint256 balance;

        // for normal address this field is null
        // for contract address this field is creator of this contract
        private final HexBytes createdBy;

        // hash code of contract code
        // if the account contains none contract, contract hash will be null
        private final HexBytes contractHash;

        // root hash of contract db
        // if the account is not contract account, this field will be null
        private final HexBytes storageRoot;

        static AccountView fromAccount(Account account) {
            return new AccountView(account.getAddress(), account.getNonce(), account.getBalance(),
                account.getCreatedBy(), account.getContractHash(),
                account.getStorageRoot());
        }
    }
}

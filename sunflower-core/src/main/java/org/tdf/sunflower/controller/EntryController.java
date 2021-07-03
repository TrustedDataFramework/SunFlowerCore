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
import org.tdf.sunflower.facade.*;
import org.tdf.sunflower.net.Peer;
import org.tdf.sunflower.net.PeerServer;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.AccountTrie;
import org.tdf.sunflower.state.Address;
import org.tdf.sunflower.sync.SyncManager;
import org.tdf.sunflower.types.*;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

    private final ObjectMapper objectMapper;

    private final RepositoryService repo;

    private final SyncManager syncManager;

    private final ConsensusEngine consensusEngine;

    private final Miner miner;

    private final ApplicationContext ctx;

    @SneakyThrows
    private Stat createState() {
        AppConfig c = AppConfig.get();

        Stat.StatBuilder builder = Stat.builder();
        OperatingSystemMXBean osMxBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        Block best;
        List<Block> blocks;

        try (RepositoryReader rd = repo.getReader()) {
            best = rd.getBestBlock();
            blocks = rd.getBlocksBetween
                (Math.max(0, best.getHeight() - 10), best.getHeight(), Integer.MAX_VALUE, false);
        }

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

        String diff = "";

        return builder
            .cpu(osMxBean.getSystemLoadAverage())
            .memoryUsed(osMxBean.getTotalPhysicalMemorySize() - osMxBean.getFreePhysicalMemorySize())
            .totalMemory(osMxBean.getTotalPhysicalMemorySize())
            .averageGasPrice(
                totalTransactions == 0 ? Uint256.ZERO : totalGasPrice.div(Uint256.of(totalTransactions)))
            .mining(blocks.stream().anyMatch(
                x -> x.getBody().size() > 0 && x.getBody().get(0).getReceiveHex().equals(miner.getMinerAddress())))
            .currentDifficulty(diff).transactionPoolSize(0)
            .consensus(consensusEngine.getName())
            .gasPrice(c.getVmGasPrice().longValue())
            .build()
            ;
    }

    private <T> T getBlockOrHeader(RepositoryReader rd, String hashOrHeight, Function<Long, T> func,
                                   Function<HexBytes, T> func1) {
        Long height = null;
        try {
            height = Long.parseLong(hashOrHeight);
            Header h = rd.getBestHeader();
            while (height < 0) {
                height += h.getHeight() + 1;
            }
        } catch (Exception ignored) {

        }
        if (height != null) {
            final long finalHeight = height;
            return func.apply(height);
        }
        return func1.apply(HexBytes.fromHex(hashOrHeight));
    }

    @GetMapping(value = "/block/{hashOrHeight}", produces = MediaType.APPLICATION_JSON_VALUE)
    public BlockV1 getBlock(@PathVariable String hashOrHeight) {
        try (RepositoryReader rd = repo.getReader()) {
            return BlockV1.fromV2(getBlockOrHeader(rd, hashOrHeight, rd::getCanonicalBlock, rd::getBlockByHash));
        }
    }

    @GetMapping(value = "/header/{hashOrHeight}", produces = MediaType.APPLICATION_JSON_VALUE)
    public HeaderV1 getHeaders(@PathVariable String hashOrHeight) {
        try (RepositoryReader rd = repo.getReader()) {
            return HeaderV1.fromV2(getBlockOrHeader(rd, hashOrHeight, rd::getCanonicalHeader, rd::getHeaderByHash));
        }
    }

    @GetMapping(value = "/transaction/{hash}", produces = MediaType.APPLICATION_JSON_VALUE)
    public TransactionV1 getTransaction(@PathVariable String hash) {
        try (RepositoryReader rd = repo.getReader()) {
            return TransactionV1.fromV2(
                rd.getTransaction(HexBytes.fromHex(hash))
            );
        }
    }

    @GetMapping(value = "/account/{addressOrPublicKey}", produces = MediaType.APPLICATION_JSON_VALUE)
    public AccountView getAccount(@PathVariable String addressOrPublicKey) throws Exception {
        HexBytes addressHex = Address.of(addressOrPublicKey);
        try (RepositoryReader rd = repo.getReader()) {
            Account a = accountTrie.get(rd.getBestHeader().getStateRoot(), addressHex);
            if (a == null)
                a = Account.emptyAccount(Uint256.ZERO);
            return AccountView.fromAccount(addressHex, a);
        }
    }

    @GetMapping(value = "/peers", produces = MediaType.APPLICATION_JSON_VALUE)
    public PeersInfo peers() {
        return new PeersInfo(peerServer.getPeers(), peerServer.getBootstraps());
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
        return poa
                .farmBaseTransactions
                .asMap()
                .values()
                .stream()
                .map(RLPUtil::encode)
                .collect(Collectors.toList());
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

        static AccountView fromAccount(HexBytes address, Account account) {
            return new AccountView(address, account.getNonce(), account.getBalance(),
                Address.empty(), account.getContractHash(),
                account.getStorageRoot());
        }
    }
}

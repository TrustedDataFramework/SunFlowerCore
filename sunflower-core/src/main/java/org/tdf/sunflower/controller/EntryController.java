package org.tdf.sunflower.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.tdf.common.trie.Trie;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.GlobalConfig;
import org.tdf.sunflower.consensus.vrf.contract.VrfPreBuiltContract;
import org.tdf.sunflower.consensus.vrf.util.VrfUtil;
import org.tdf.sunflower.facade.ConsensusEngine;
import org.tdf.sunflower.facade.SunflowerRepository;
import org.tdf.sunflower.facade.TransactionPool;
import org.tdf.sunflower.net.Peer;
import org.tdf.sunflower.net.PeerServer;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.AccountTrie;
import org.tdf.sunflower.state.Address;
import org.tdf.sunflower.sync.SyncManager;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.types.PagedView;
import org.tdf.sunflower.types.Transaction;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.tdf.sunflower.state.Constants.VRF_BIOS_CONTRACT_ADDR;

@RestController
@AllArgsConstructor
@RequestMapping("/rpc")
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
    public Transaction getTransaction(@PathVariable String hash) throws Exception {
        return repository.getTransactionByHash(HexBytes.decode(hash))
                .orElseThrow(() -> new RuntimeException("transaction " + hash + " not exists"));
    }

    @GetMapping(value = "/account/{addressOrPublicKey}", produces = MediaType.APPLICATION_JSON_VALUE)
    public AccountView getAccount(@PathVariable String addressOrPublicKey) throws Exception {
        HexBytes addressHex = Address.of(addressOrPublicKey);
        return accountTrie.get(sunflowerRepository.getBestHeader().getStateRoot().getBytes(), addressHex)
                .map(AccountView::fromAccount)
                .orElse(new AccountView(addressHex, 0, 0, HexBytes.EMPTY, HexBytes.EMPTY, HexBytes.empty()));
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
        return errors.isEmpty() ? Response
                .newSuccessFul(
                        ts.stream().map(Transaction::getHash)
                        .collect(Collectors.toList())
                )
                : Response.newFailed(Response.Code.INTERNAL_ERROR, String.join("\n", errors));
    }

    @GetMapping(value = "/contract/{address}", produces = MediaType.APPLICATION_JSON_VALUE)
    public HexBytes getContract(@PathVariable("address") final String address,
                                @RequestParam(value = "parameters") String arguments) throws Exception {
        HexBytes addressHex = Address.of(address);
        HexBytes args = HexBytes.fromHex(arguments);
        Header h = sunflowerRepository.getBestHeader();
        byte[] result = accountTrie.view(h.getStateRoot().getBytes(), addressHex, args);
        return HexBytes.fromBytes(result);
    }

    @PostMapping(value = "/contract/{address}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Object rpcQuery(@PathVariable("address") final String address,
                           @RequestBody(required = false) JsonNode body) throws Exception {
        return consensusEngine.rpcQuery(HexBytes.fromHex(address), body);
    }

    @GetMapping(value = "/contract/vrf/{address}", produces = MediaType.APPLICATION_JSON_VALUE)
    public HexBytes getVrfContract(@PathVariable("address") final String address,
                                   String depAddr) throws Exception {
        HexBytes contractAddressHex = Address.of(address);
        Header h = sunflowerRepository.getBestHeader();
        if (VRF_BIOS_CONTRACT_ADDR.equals(address)) {
            if (depAddr != null && !depAddr.equals("")) {
                HexBytes depositAddress = HexBytes.fromHex(depAddr);
                return HexBytes.fromBytes(VrfUtil.getFromContractStorage(contractAddressHex, h, depositAddress.getBytes(), accountTrie,
                        contractStorageTrie));
            } else {
                return HexBytes.fromBytes(VrfUtil.getFromContractStorage(contractAddressHex, h,
                        VrfPreBuiltContract.TOTAL_KEY, accountTrie, contractStorageTrie));
            }

        }
        return HexBytes.fromBytes("NOT_VRF_CONTRACT_ADDRESS".getBytes());
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
        private long nonce;

        // the balance of account
        // for contract account, this field is zero
        private long balance;

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

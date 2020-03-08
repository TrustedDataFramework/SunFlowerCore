package org.tdf.sunflower.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.GlobalConfig;
import org.tdf.sunflower.account.Address;
import org.tdf.sunflower.facade.SunflowerRepository;
import org.tdf.sunflower.facade.TransactionPool;
import org.tdf.sunflower.net.Peer;
import org.tdf.sunflower.net.PeerServer;
import org.tdf.sunflower.proto.Sunflower;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.AccountTrie;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.types.Transaction;
import org.tdf.sunflower.types.UnmodifiableTransaction;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


@RestController
@AllArgsConstructor
@RequestMapping("/rpc")
public class EntryController {
    private AccountTrie accountTrie;

    private GlobalConfig config;

    private PeerServer peerServer;

    private TransactionPool pool;

    private SunflowerRepository sunflowerRepository;

    private ObjectMapper objectMapper;

    private SunflowerRepository repository;

    @GetMapping(value = "/block/{hashOrHeight}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Block getBlock(@PathVariable String hashOrHeight) throws Exception {
        try{
            long height = Long.parseLong(hashOrHeight);
            Header h = repository.getBestHeader();
            while (height < 0){
                height += h.getHeight() + 1;
            }
            final long finalHeight = height;
            return repository.getCanonicalBlock(height)
                    .orElseThrow(() -> new RuntimeException("block at height " + finalHeight + " not found"));
        }catch (Exception ignored){

        }
        return repository.getBlock(HexBytes.decode(hashOrHeight))
                .orElseThrow(() -> new RuntimeException("block of hash " + hashOrHeight + " not found"));
    }

    @GetMapping(value = "/header/{hashOrHeight}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Header getHeaders(@PathVariable String hashOrHeight) throws Exception {
        try{
            long height = Long.parseLong(hashOrHeight);
            Header h = repository.getBestHeader();
            while (height < 0){
                height += h.getHeight() + 1;
            }
            final long finalHeight = height;
            return repository.getCanonicalHeader(height)
                    .orElseThrow(() -> new RuntimeException("header at height " + finalHeight + " not found"));
        }catch (Exception ignored){

        }
        return repository.getHeader(HexBytes.decode(hashOrHeight))
                .orElseThrow(() -> new RuntimeException("header of hash " + hashOrHeight + " not found"));
    }

    @GetMapping(value = "/transaction/{hash}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Transaction getTransaction(@PathVariable String hash) throws Exception {
        return repository
                .getTransactionByHash(HexBytes.decode(hash))
                .orElseThrow(() -> new RuntimeException("transaction " + hash + " not exists"));
    }


    @GetMapping(value = "/account/{addressOrPublicKey}", produces = MediaType.APPLICATION_JSON_VALUE)
    public AccountView getAccount(@PathVariable String addressOrPublicKey) throws Exception {
        HexBytes addressHex = Address.of(addressOrPublicKey);
        return accountTrie
                .get(sunflowerRepository.getBestHeader().getStateRoot().getBytes(), addressHex)
                .map(AccountView::fromAccount)
                .orElse(new AccountView(addressHex, 0, 0, HexBytes.EMPTY, HexBytes.EMPTY, HexBytes.empty()))
                ;
    }

    // TODO: enclose this config
    @GetMapping(value = "/config", produces = MediaType.APPLICATION_JSON_VALUE)
    public GlobalConfig config() {
        return config;
    }

    @GetMapping(value = "/peers", produces = MediaType.APPLICATION_JSON_VALUE)
    public PeersInfo peers() {
        return new PeersInfo(
                peerServer.getPeers(),
                peerServer.getBootStraps()
        );
    }

    @GetMapping(value = "/pool", produces = MediaType.APPLICATION_JSON_VALUE)
    public Collection<Transaction> getPool(@ModelAttribute PoolQuery poolQuery) {
        switch (poolQuery.getStatus()){
            case "pending":
                return pool.get(poolQuery);
            case "dropped":
                return pool.getDropped(poolQuery);
            default:
                throw new RuntimeException("unknown status " + poolQuery.getStatus());
        }
    }


    @PostMapping(value = "/transaction", produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<String> sendTransaction(@RequestBody JsonNode node) {
        if(node.isArray()){
            pool.collect(Arrays.asList(objectMapper.convertValue(node, Transaction[].class)));
            return Response.newSuccessFul("ok");
        }
        pool.collect(objectMapper.convertValue(node, Transaction.class));
        return Response.newSuccessFul("ok");
    }

    @GetMapping(value = "/contract/{address}", produces = MediaType.APPLICATION_JSON_VALUE)
    public HexBytes getContract(
            @PathVariable("address") final String address,
            @RequestParam(value = "parameters") String arguments
    ) throws Exception {
        HexBytes addressHex = Address.of(address);
        HexBytes args = HexBytes.fromHex(arguments);
        Header h = sunflowerRepository.getBestHeader();
        byte[] result = accountTrie.view(h.getStateRoot().getBytes(), addressHex, args);
        return HexBytes.fromBytes(result);
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
            return new AccountView(
                    account.getAddress(), account.getNonce(), account.getBalance(),
                    account.getCreatedBy(), HexBytes.fromBytes(account.getContractHash()),
                    HexBytes.fromBytes(account.getStorageRoot())
            );
        }
    }
}

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
                .orElse(new AccountView(addressHex, 0, 0))
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

    @Builder
    @Getter
    static class AccountView {
        private HexBytes address;
        private long balance;
        private long nonce;

        static AccountView fromAccount(Account account) {
            return builder()
                    .address(account.getAddress())
                    .balance(account.getBalance())
                    .nonce(account.getNonce())
                    .build();
        }
    }
}

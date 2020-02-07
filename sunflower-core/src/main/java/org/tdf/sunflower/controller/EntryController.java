package org.tdf.sunflower.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.GlobalConfig;
import org.tdf.sunflower.account.Address;
import org.tdf.sunflower.facade.SunflowerRepository;
import org.tdf.sunflower.facade.TransactionPool;
import org.tdf.sunflower.net.Peer;
import org.tdf.sunflower.net.PeerServer;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.AccountTrie;
import org.tdf.sunflower.types.Transaction;

import java.util.Collections;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

@RestController
@AllArgsConstructor
public class EntryController {
    private AccountTrie accountTrie;

    private GlobalConfig config;

    private PeerServer peerServer;

    private TransactionPool pool;

    private SunflowerRepository sunflowerRepository;

    private ObjectMapper objectMapper;


    @GetMapping(value = "/hello", produces = MediaType.APPLICATION_JSON_VALUE)
    public byte[] hello() {
        return "hello".getBytes(UTF_8);
    }

    @GetMapping(value = "/man", produces = MediaType.APPLICATION_JSON_VALUE)
    public HexBytes man() throws Exception {
        return HexBytes.fromHex("ffffffff");
    }

    @GetMapping(value = "/exception", produces = MediaType.APPLICATION_JSON_VALUE)
    public String exception() throws RuntimeException {
        throw new RuntimeException("error");
    }

    @GetMapping(value = "/account/{addressOrPublicKey}", produces = MediaType.APPLICATION_JSON_VALUE)
    public AccountView getAccount(@PathVariable String addressOrPublicKey) throws Exception {
        HexBytes addressHex = Address.of(addressOrPublicKey);
        return accountTrie
                .get(sunflowerRepository.getLastConfirmed().getStateRoot().getBytes(), addressHex)
                .map(AccountView::fromAccount)
                .orElse(new AccountView(addressHex, 0))
                ;
    }

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
    public Response<String> sendTransaction(@RequestBody Transaction transaction) {
        pool.collect(Collections.singleton(transaction));
        return Response.newSuccessFul("ok");
    }

    @GetMapping(value = "/contract/{address}/{method}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ContractView getContract(
            @PathVariable("address") final String address,
            @PathVariable("method") String method,
            @RequestParam(value = "parameters", required = false) String parameters
    ) throws Exception {
        HexBytes addressHex = Address.of(address);
        byte[] params = parameters == null ? new byte[0] : HexBytes.fromHex(parameters).getBytes();
        byte[] hash = sunflowerRepository.getBestBlock().getHash().getBytes();
        Account a = accountTrie
                .get(sunflowerRepository.getLastConfirmed().getStateRoot().getBytes(), addressHex)
                .filter(Account::containsContract)
                .orElseThrow(() -> new RuntimeException("the address " + addressHex + " has no contract deployed"));
        ContractView view = new ContractView();
        byte[] result = a.view(method, params);
        try {
            view.json = objectMapper.readValue(result, JsonNode.class);
        } catch (Exception ignored) {
        }
        view.raw = HexBytes.fromBytes(result);
        return view;
    }

    private static class ContractView {
        JsonNode json;
        HexBytes raw;
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

        static AccountView fromAccount(Account account) {
            return builder()
                    .address(account.getAddress())
                    .balance(account.getBalance())
                    .build();
        }
    }
}

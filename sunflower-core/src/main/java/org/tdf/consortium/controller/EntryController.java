package org.tdf.consortium.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.tdf.common.*;
import org.tdf.consortium.account.PublicKeyHash;
import org.wisdom.consortium.GlobalConfig;
import org.wisdom.consortium.state.Account;

import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

@RestController
public class EntryController {
    @Autowired
    private StateRepository repository;

    @Autowired
    private GlobalConfig config;

    @Autowired
    private PeerServer peerServer;

    @Autowired
    private TransactionPool pool;

    @Autowired
    private ConsortiumRepository consortiumRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @GetMapping(value = "/hello", produces = MediaType.APPLICATION_JSON_VALUE)
    public Object hello() {
        return "hello".getBytes(UTF_8);
    }

    @GetMapping(value = "/man", produces = MediaType.APPLICATION_JSON_VALUE)
    public Object man() throws Exception {
        return new HexBytes(Hex.decodeHex("ffffffff".toCharArray()));
    }

    @GetMapping(value = "/exception", produces = MediaType.APPLICATION_JSON_VALUE)
    public Object exception() throws RuntimeException {
        throw new RuntimeException("error");
    }

    @GetMapping(value = "/account/{address}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Object getAccount(@PathVariable String address) throws Exception {
        PublicKeyHash publicKeyHash = PublicKeyHash
                .from(address)
                .orElseThrow(() -> new RuntimeException("invalid format " + address));
        return AccountView.fromAccount(repository.getLastConfirmed(publicKeyHash.getAddress(), Account.class));
    }

    @GetMapping(value = "/config", produces = MediaType.APPLICATION_JSON_VALUE)
    public Object config() {
        return config;
    }

    @GetMapping(value = "/peers", produces = MediaType.APPLICATION_JSON_VALUE)
    public Object peers() {
        return new PeersInfo(
                peerServer.getPeers(),
                peerServer.getBootStraps()
                );
    }

    @PostMapping(value = "/transaction", produces = MediaType.APPLICATION_JSON_VALUE)
    public Object sendTransaction(@RequestBody Transaction transaction){
        pool.collect(transaction);
        return Response.newSuccessFul("ok");
    }

    @GetMapping(value = "/contract/{address}/{method}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Object getContract(
            @PathVariable("address") final String address,
            @PathVariable("method") String method,
            @RequestParam(value = "parameters", required = false) String parameters
    ) throws Exception{
        PublicKeyHash publicKeyHash = PublicKeyHash.from(address).orElseThrow(() -> new RuntimeException("invalid format " + address));
        byte[] params = parameters == null ? new byte[0] : HexBytes.parse(parameters).getBytes();
        byte[] hash = consortiumRepository.getBestBlock().getHash().getBytes();
        Account a = repository.get(publicKeyHash.getAddress(), hash, Account.class)
                .filter(x -> x.getBinaryContract() != null)
                .orElseThrow(() -> new RuntimeException("the address " + address + " has no contract deployed"));
        ContractView view = new ContractView();
        byte[] result = a.view(method, params);
        try{
            view.json = objectMapper.readValue(result, JsonNode.class);
        }catch (Exception ignored){}
        view.raw = new HexBytes(result);
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
    static class AccountView{
        private String address;
        private String publicKeyHash;
        private long balance;

        static AccountView fromAccount(Account account){
            return builder()
                    .address(account.getIdentifier())
                    .balance(account.getBalance())
                    .publicKeyHash(account.getPublicKeyHash().getHex())
                    .build();
        }
    }
}

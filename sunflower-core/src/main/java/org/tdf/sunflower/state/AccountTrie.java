package org.tdf.sunflower.state;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.tdf.common.serialize.Codecs;
import org.tdf.common.store.*;
import org.tdf.common.trie.SecureTrie;
import org.tdf.common.trie.Trie;
import org.tdf.common.util.BigEndian;
import org.tdf.common.util.ByteArrayMap;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.Start;
import org.tdf.sunflower.facade.BasicMessageQueue;
import org.tdf.sunflower.types.CryptoContext;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.types.Transaction;
import org.tdf.sunflower.vm.abi.Context;
import org.tdf.sunflower.vm.abi.ContractCall;
import org.tdf.sunflower.vm.hosts.Limit;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@Slf4j(topic = "trie")
@AllArgsConstructor
public class AccountTrie extends AbstractStateTrie<HexBytes, Account> implements ForkedStateTrie<HexBytes, Account> {
    private Trie<HexBytes, Account> trie;
    private Trie<byte[], byte[]> contractStorageTrie;
    private Store<byte[], byte[]> contractCodeStore;
    private BasicMessageQueue messageQueue;
    private List<PreBuiltContract> preBuiltContracts;
    private Map<HexBytes, Bios> biosList;
    private Map<HexBytes, Account> genesisStates;
    private Map<HexBytes, PreBuiltContract> preBuiltContractAddresses;
    private DatabaseStore db;
    private Store<byte[], byte[]> trieStore;
    private HexBytes genesisRoot;

    // memory cached
    private Map<byte[], byte[]> trieCache;
    private Map<byte[], byte[]> contractStorageCache;
    private Map<byte[], byte[]> contractCodeCache;
    private Trie<HexBytes, Account> dirtyTrie;

    @SneakyThrows
    public AccountTrie(
            DatabaseStore db,
            Store<byte[], byte[]> contractCodeStore,
            Trie<byte[], byte[]> contractStorageTrie,
            BasicMessageQueue messageQueue,
            Map<HexBytes, Account> genesisStates,
            List<PreBuiltContract> preBuiltContracts,
            List<Bios> biosList,
            boolean secure
    ) {
        this.db = db;
        this.trieStore = new NoDeleteBatchStore<>(db);

        this.trie = Trie.<HexBytes, Account>builder()
                .hashFunction(CryptoContext::hash)
                .store(trieStore)
                .keyCodec(Codecs.newRLPCodec(HexBytes.class))
                .valueCodec(Codecs.newRLPCodec(Account.class))
                .build();

        if (secure)
            trie = new SecureTrie<>(trie, CryptoContext::hash);

        this.contractStorageTrie = contractStorageTrie;
        this.contractCodeStore = contractCodeStore;
        this.messageQueue = messageQueue;
        this.preBuiltContracts = preBuiltContracts;
        this.biosList = biosList.stream().collect(
                Collectors.toMap(x -> x.getGenesisAccount().getAddress(), Function.identity())
        );
        this.genesisStates = new HashMap<>(genesisStates);

        List<CommonUpdater> commonUpdaters = new ArrayList<>();
        commonUpdaters.addAll(preBuiltContracts);
        commonUpdaters.addAll(biosList);
        this.preBuiltContractAddresses = new HashMap<>();

        for (CommonUpdater updater : commonUpdaters) {
            Account genesisAccount = updater.getGenesisAccount();
            Trie<byte[], byte[]> trie = contractStorageTrie.revert();
            for (Map.Entry<byte[], byte[]> entry : updater.getGenesisStorage().entrySet()) {
                trie.put(entry.getKey(), entry.getValue());
            }
            byte[] root = trie.commit();
            trie.flush();
            genesisAccount.setStorageRoot(root);
            if (updater instanceof PreBuiltContract) {
                preBuiltContractAddresses.put(updater.getGenesisAccount().getAddress(), (PreBuiltContract) updater);
            }
            this.genesisStates.put(updater.getGenesisAccount().getAddress(), genesisAccount);
        }

        // sync to genesis
        Trie<HexBytes, Account> tmp = this.trie.revert();
        genesisStates.forEach(tmp::put);
        this.genesisRoot = HexBytes.fromBytes(tmp.commit());
        log.info("genesis states = {}", Start.MAPPER.writeValueAsString(genesisStates));
        log.info("genesis state root = " + genesisRoot);
        tmp.flush();
    }

    private static void require(boolean b, String msg) throws RuntimeException {
        if (!b) {
            throw new RuntimeException(msg);
        }
    }

    private Trie<byte[], byte[]> getDirtyContractStorageTrie(byte[] storageRoot) {
        return contractStorageTrie.revert(
                storageRoot,
                new NoDeleteCachedStore<>(
                        contractStorageTrie.getStore(),
                        () -> this.contractStorageCache
                )
        );
    }

//    public byte[] view(byte[] stateRoot, HexBytes address, HexBytes args) {
//        Store<byte[], byte[]> cache = new CachedStore<>(getTrieStore(), ByteArrayMap::new);
//        Store<HexBytes, Account> trie = getTrie().revert(stateRoot, cache);
//        Store<byte[], byte[]> contractCodeStore = new CachedStore<>(this.contractCodeStore, ByteArrayMap::new);
//
//        ContractCall call = new ContractCall();
//        return hosts.getResult();
//    }

    private Store<byte[], byte[]> getDirtyContractCodeStore() {
        return new CachedStore<>(this.contractCodeStore, () -> this.contractCodeCache);
    }

    @Override
    DatabaseStore getDB() {
        return this.db;
    }

    @Override
    protected Trie<HexBytes, Account> commitInternal(byte[] parentRoot, Map<HexBytes, Account> data) {
        data.remove(Constants.FEE_ACCOUNT_ADDR);
        return super.commitInternal(parentRoot, data);
    }

    @Override
    public ForkedStateTrie<HexBytes, Account> fork(byte[] parentRoot) {
        Map<byte[], byte[]> trieCache = new ByteArrayMap<>();
        Store<byte[], byte[]> cachedTrieStore = new CachedStore<>(trie.getStore(), () -> trieCache);
        Map<byte[], byte[]> contractStorageCache = new ByteArrayMap<>();
        Map<byte[], byte[]> contractCodeCache = new ByteArrayMap<>();
        return new AccountTrie(
                trie,
                contractStorageTrie,
                this.contractCodeStore,
                messageQueue,
                preBuiltContracts,
                biosList,
                genesisStates,
                preBuiltContractAddresses,
                db,
                this.trieStore,
                genesisRoot,
                trieCache,
                contractStorageCache,
                contractCodeCache,
                trie.revert(parentRoot, cachedTrieStore)
        );
    }

    public void updateFeeAccount(long fee) {
        Map<HexBytes, Account> states = this.dirtyTrie.asMap();
        Account feeAccount = states.get(Constants.FEE_ACCOUNT_ADDR);
        feeAccount.setBalance(SafeMath.add(feeAccount.getBalance(), fee));
        states.put(feeAccount.getAddress(), feeAccount);
    }

    public void increaseNonce(Transaction tx) {
        Map<HexBytes, Account> states = this.dirtyTrie.asMap();
        Account state = states.get(tx.getFromAddress());
        if (state.getNonce() + 1 != tx.getNonce())
            throw new RuntimeException("the nonce of transaction should be " + (state.getNonce() + 1)
                    + " while " + tx.getNonce() + " received");
        state.setNonce(tx.getNonce());
        states.put(state.getAddress(), state);
    }

    public void updateTransfer(HexBytes fromAddr, HexBytes toAddr, long amount, long fee) {
        Map<HexBytes, Account> states = this.dirtyTrie.asMap();

        if (amount == 0)
            throw new RuntimeException("transfer amount = 0");

        Account from = states.get(fromAddr);
        from.setBalance(SafeMath.sub(from.getBalance(), SafeMath.add(amount, fee)));

        Account to = states.get(toAddr);
        if (to.getCreatedBy() != null && !to.getCreatedBy().isEmpty())
            throw new RuntimeException("transfer to contract address is not allowed");

        to.setBalance(SafeMath.add(to.getBalance(), amount));
        states.put(fromAddr, from);
        states.put(toAddr, to);
        updateFeeAccount(fee);
    }

    public void update(Header header, Transaction tx) {
        Map<HexBytes, Account> states = this.dirtyTrie.asMap();
        if (tx.getType() == Transaction.Type.COIN_BASE.code && header.getHeight() != tx.getNonce()) {
            throw new RuntimeException("nonce of coinbase transaction should be " + header.getHeight());
        }
        states.putIfAbsent(Constants.FEE_ACCOUNT_ADDR, Account.emptyAccount(Constants.FEE_ACCOUNT_ADDR));
        switch (Transaction.Type.TYPE_MAP.get(tx.getType())) {
            case TRANSFER: {
                states.putIfAbsent(tx.getTo(), Account.emptyAccount(tx.getTo()));
                increaseNonce(tx);
                updateTransfer(
                        tx.getFromAddress(),
                        tx.getTo(),
                        tx.getAmount(),
                        tx.getFee()
                );
                return;
            }
            case COIN_BASE: {
                states.putIfAbsent(tx.getTo(), Account.emptyAccount(tx.getTo()));
                updateCoinBase(header, tx);
                return;
            }
            case CONTRACT_DEPLOY:
                updateDeploy(states, header, tx);
                return;
            case CONTRACT_CALL:
                states.putIfAbsent(tx.getFromAddress(), Account.emptyAccount(tx.getFromAddress()));
                increaseNonce(tx);
                updateContractCall(header, tx);
                return;
        }
        throw new RuntimeException("unknown type " + tx.getType());
    }


    private void updateCoinBase(Header header, Transaction t) {
        Map<HexBytes, Account> accounts = this.dirtyTrie.asMap();

        Account to = accounts.get(t.getTo());
        to.setBalance(SafeMath.add(to.getBalance(), t.getAmount()));
        accounts.put(to.getAddress(), to);

        for (Bios bios : biosList.values()) {
            Account a = accounts.get(bios.getGenesisAccount().getAddress());
            Trie<byte[], byte[]> before = getDirtyContractStorageTrie(a.getStorageRoot());
            bios.update(header, before);
            byte[] root = before.commit();
            before.flush();
            a.setStorageRoot(root);
            accounts.put(a.getAddress(), a);
        }
    }

    private void updateDeploy(Map<HexBytes, Account> accounts, Header header, Transaction t) {


        int binaryLen = BigEndian.decodeInt32(t.getPayload().slice(0, 4).getBytes());
        byte[] binary = t.getPayload().slice(4, 4 + binaryLen).getBytes();
        byte[] parameters = t.getPayload().slice(4 + binaryLen).getBytes();


        Limit limit = new Limit();


        // execute constructor of contract
        ContractCall contractCall = new ContractCall(
                accounts, header,
                t, this::getDirtyContractStorageTrie,
                messageQueue, getDirtyContractCodeStore(),
                limit, 0, t.getFromAddress(),
                false
        );

        contractCall.call(
                HexBytes.fromBytes(binary),
                "init",
                parameters,
                t.getAmount()
        );


        // restore from map
        Account createdBy = accounts.get(t.getFromAddress());
        Account contractAccount = accounts.get(t.createContractAddress());

        // estimate gas
        long gas = SafeMath.add(t.getPayload().size() / 1024, limit.getGas());
        long fee = SafeMath.mul(gas, t.getGasPrice());

        accounts.put(createdBy.getAddress(), createdBy);
        updateFeeAccount(fee);
        log.info("deploy contract at " + contractAccount.getAddress() + " success");
    }


    private void updateContractCall(Header header, Transaction t) {
        Map<HexBytes, Account> accounts = getDirtyTrie().asMap();
        Account contractAccount = accounts.get(t.getTo());

        if (preBuiltContractAddresses.containsKey(contractAccount.getAddress())) {
            long fee = SafeMath.mul(10, t.getGasPrice());
            Trie<byte[], byte[]> before =
                    getDirtyContractStorageTrie(contractAccount.getStorageRoot());
            PreBuiltContract updater = preBuiltContractAddresses.get(contractAccount.getAddress());
            updater.update(header, t, accounts, before);
            byte[] root = before.commit();
            contractAccount.setStorageRoot(root);
            updateFeeAccount(fee);
            return;
        }

        // execute method
        Limit limit = new Limit();
        ContractCall contractCall = new ContractCall(
                accounts, header,
                t, this::getDirtyContractStorageTrie,
                messageQueue,
                this.getDirtyContractCodeStore(),
                limit, 0, t.getFromAddress(),
                false
        );

        contractCall.call(
                contractAccount.getAddress(),
                Context.readMethod(t.getPayload()),
                Context.readParameters(t.getPayload()),
                t.getAmount()
        );

        contractAccount = accounts.get(t.getTo());
        Account caller = accounts.get(t.getFromAddress());

        long fee = SafeMath.mul(limit.getGas(), t.getGasPrice());
        caller.setBalance(SafeMath.sub(caller.getBalance(), fee));
        accounts.put(contractAccount.getAddress(), contractAccount);
        accounts.put(caller.getAddress(), caller);
        updateFeeAccount(fee);
    }

    @Override
    public Account remove(HexBytes key) {
        Account a = getDirtyTrie().get(key)
                .orElse(null);
        getDirtyTrie().remove(key);
        return a;
    }

    @Override
    public Optional<Account> get(HexBytes bytes) {
        return getDirtyTrie().get(bytes);
    }

    @Override
    public byte[] commit() {
        return getDirtyTrie().commit();
    }

    @Override
    public void flush() {
        getDirtyTrie().flush();
        getDirtyContractCodeStore().flush();
        getDirtyContractCodeStore().flush();
    }

    @Override
    public void put(HexBytes key, Account value) {
        getDirtyTrie().put(key, value);
    }

    public byte[] call(HexBytes address, HexBytes args) {
        // execute method
        Limit limit = new Limit();
        ContractCall contractCall = new ContractCall(
                this.dirtyTrie.asMap(), null,
                null, this::getDirtyContractStorageTrie,
                messageQueue,
                this.getDirtyContractCodeStore(),
                limit, 0, null,
                true
        );

        String method = Context.readMethod(args);
        byte[] parameters = Context.readParameters(args);
        return contractCall.call(
                address,
                method,
                parameters, 0
        );
    }
}

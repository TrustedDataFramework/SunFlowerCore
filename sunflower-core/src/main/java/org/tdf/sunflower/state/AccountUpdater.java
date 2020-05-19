package org.tdf.sunflower.state;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.tdf.common.store.CachedStore;
import org.tdf.common.store.Store;
import org.tdf.common.trie.Trie;
import org.tdf.common.util.ByteArrayMap;
import org.tdf.common.util.HexBytes;
import org.tdf.lotusvm.ModuleInstance;
import org.tdf.sunflower.crypto.CryptoContext;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.types.Transaction;
import org.tdf.sunflower.vm.abi.Context;
import org.tdf.sunflower.vm.hosts.ContractDB;
import org.tdf.sunflower.vm.hosts.GasLimit;
import org.tdf.sunflower.vm.hosts.Hosts;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * where state transition happens
 */
@Slf4j(topic = "account")
public class AccountUpdater extends AbstractStateUpdater<HexBytes, Account> {
    @Getter
    private final Map<HexBytes, Account> genesisStates;

    @Qualifier("contractCodeStore")
    private final Store<byte[], byte[]> contractStore;

    @Qualifier("contractStorageTrie")
    private final Trie<byte[], byte[]> storageTrie;

    private final List<PreBuiltContract> preBuiltContracts;

    private final Map<HexBytes, PreBuiltContract> preBuiltContractAddresses;

    private final Map<HexBytes, Bios> biosList;

    public AccountUpdater(Map<HexBytes, Account> genesisStates, Store<byte[], byte[]> contractStore,
                          Trie<byte[], byte[]> storageTrie, List<PreBuiltContract> preBuiltContracts, List<Bios> biosList) {
        this.genesisStates = new HashMap<>(genesisStates);
        this.contractStore = contractStore;
        this.storageTrie = storageTrie;
        this.preBuiltContracts = preBuiltContracts;
        this.biosList = biosList.stream().collect(
                Collectors.toMap(x -> x.getGenesisAccount().getAddress(), Function.identity())
        );
        List<CommonUpdater> commonUpdaters = new ArrayList<>();
        commonUpdaters.addAll(preBuiltContracts);
        commonUpdaters.addAll(biosList);
        preBuiltContractAddresses = new HashMap<>();
        for (CommonUpdater updater : commonUpdaters) {
            Account genesisAccount = updater.getGenesisAccount();
            Trie<byte[], byte[]> trie = storageTrie.revert();
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

    }

    @Override
    public Set<HexBytes> getRelatedKeys(Transaction transaction, Map<HexBytes, Account> store) {
        switch (Transaction.Type.TYPE_MAP.get(transaction.getType())) {
            case COIN_BASE: {
                Set<HexBytes> ret = new HashSet<>(biosList.keySet());
                ret.add(transaction.getTo());
                return ret;
            }
            case TRANSFER: {
                if (!store.containsKey(transaction.getFromAddress()))
                    throw new RuntimeException("account " + transaction.getFromAddress() + " not exists");
                Set<HexBytes> ret = new HashSet<>();
                ret.add(transaction.getFromAddress());
                ret.add(transaction.getTo());
                return ret;
            }
            case CONTRACT_DEPLOY: {
                Set<HexBytes> ret = new HashSet<>();
                ret.add(transaction.getFromAddress());
                Account a = store.get(transaction.createContractAddress());
                if (a != null && !a.isFresh())
                    throw new RuntimeException("contract " + transaction.createContractAddress() + " exists");
                ret.add(transaction.createContractAddress());
                return ret;
            }
            case CONTRACT_CALL: {
                Set<HexBytes> ret = new HashSet<>();
                if (!store.containsKey(transaction.getFromAddress()))
                    throw new RuntimeException("account " + transaction.getFromAddress() + " not exists");
                if (!store.containsKey(transaction.getTo()))
                    throw new RuntimeException("contract " + transaction.getFromAddress() + " not exists");
                ret.add(transaction.getFromAddress());
                ret.add(transaction.getTo());
                ret.add(store.get(transaction.getTo()).getCreatedBy());
                return ret;
            }
        }
        throw new RuntimeException("unreachable");
    }

    @Override
    public Map<HexBytes, Account> update(Map<HexBytes, Account> states, Header header, Transaction transaction) {
        Map<HexBytes, Account> cloned = createEmptyMap();
        states.forEach((k, v) -> cloned.put(k, v.clone()));
        if (transaction.getType() == Transaction.Type.COIN_BASE.code && header.getHeight() != transaction.getNonce()) {
            throw new RuntimeException("nonce of coinbase transaction should be " + header.getHeight());
        }
        for (Account state : cloned.values()) {
            if (transaction.getType() == Transaction.Type.COIN_BASE.code)
                continue;
            if (state.getAddress().equals(transaction.getFromAddress())) {
                if (state.getNonce() + 1 != transaction.getNonce())
                    throw new RuntimeException("the nonce of transaction should be " + (state.getNonce() + 1)
                            + " while " + transaction.getNonce() + " received");
                state.setNonce(state.getNonce() + 1);
            }
        }
        switch (Transaction.Type.TYPE_MAP.get(transaction.getType())) {
            case TRANSFER:
                return updateTransfer(cloned, transaction);
            case COIN_BASE:
                return updateCoinBase(header, cloned, transaction);
            case CONTRACT_DEPLOY:
                return updateDeploy(cloned, header, transaction);
            case CONTRACT_CALL:
                return updateContractCall(cloned, header, transaction);
        }
        throw new RuntimeException("unknown type " + transaction.getType());
    }

    /**
     * create a fresh new account by address
     *
     * @param address address
     * @return a fresh new account
     */
    @Override
    public Account createEmpty(HexBytes address) {
        return new Account(address, 0, 0, HexBytes.EMPTY, null, null, true);
    }

    @Override
    public Map<HexBytes, Account> createEmptyMap() {
        return new HashMap<>();
    }

    @Override
    public Set<HexBytes> createEmptySet() {
        return new HashSet<>();
    }

    private Map<HexBytes, Account> updateTransfer(Map<HexBytes, Account> states, Transaction t) {
        for (Map.Entry<HexBytes, Account> entry : states.entrySet()) {
            Account state = entry.getValue();
            if (t.getFromAddress().equals(state.getAddress())) {
                require(state.getBalance() >= t.getAmount(), "the balance of sender is not enough");
                state.setBalance(state.getBalance() - t.getAmount());
            }
            if (t.getTo().equals(state.getAddress())) {
                if (state.getCreatedBy() != null && !state.getCreatedBy().isEmpty())
                    throw new RuntimeException("transfer to contract address is not allowed");
                state.setBalance(state.getBalance() + t.getAmount());
                if (state.getBalance() < 0)
                    throw new RuntimeException("math overflow");
            }
            if (!t.getFromAddress().equals(state.getAddress()) && !t.getTo().equals(state.getAddress()))
                throw new RuntimeException(
                        "unreachable: nor to or from " + t.getHash() + " equals to the account " + state.getAddress());
        }
        return states;
    }

    private Map<HexBytes, Account> updateCoinBase(Header header, Map<HexBytes, Account> accounts, Transaction t) {

        for (Account account : accounts.values()) {
            if (!account.getAddress().equals(t.getTo()))
                continue;
            account.setBalance(account.getBalance() + t.getAmount());
        }

        for (Bios bios : biosList.values()) {
            Account a = accounts.get(bios.getGenesisAccount().getAddress());
            Trie<byte[], byte[]> before = storageTrie.revert(a.getStorageRoot(),
                    new CachedStore<>(storageTrie.getStore(), ByteArrayMap::new));
            bios.update(header, before);
            byte[] root = before.commit();
            before.flush();
            a.setStorageRoot(root);
        }

        return accounts;
    }

    private Map<HexBytes, Account> updateDeploy(Map<HexBytes, Account> accounts, Header header, Transaction t) {
        Account contractAccount = Objects.requireNonNull(accounts.get(t.createContractAddress()));

        contractAccount.setCreatedBy(t.getFromAddress());
        contractAccount.setNonce(t.getNonce());

        // build Parameters here
        Context context = new Context(header, t, contractAccount, null);

        ContractDB contractDB = new ContractDB(storageTrie.revert(storageTrie.getNullHash(),
                new CachedStore<>(storageTrie.getStore(), ByteArrayMap::new)));

        Hosts hosts = new Hosts().withContext(context).withDB(contractDB);

        // every contract must has a init method
        ModuleInstance instance = ModuleInstance.builder().hooks(Collections.singleton(new GasLimit()))
                .binary(t.getPayload().getBytes()).hostFunctions(hosts.getAll()).build();

        byte[] contractHash = CryptoContext.digest(t.getPayload().getBytes());

        contractStore.put(contractHash, t.getPayload().getBytes());
        contractAccount.setContractHash(contractHash);

        if (instance.containsExport("init")) {
            instance.execute("init");
        }

        contractDB.getStorageTrie().commit();
        contractDB.getStorageTrie().flush();
        contractAccount.setStorageRoot(contractDB.getStorageTrie().getRootHash());
        log.info("deploy contract at " + contractAccount.getAddress() + " success");
        return accounts;
    }

    private Map<HexBytes, Account> updateContractCall(Map<HexBytes, Account> accounts, Header header, Transaction t) {
        Account contractAccount = accounts.get(t.getTo());
        for (Account account : accounts.values()) {
            if (account.getAddress().equals(t.getFromAddress())) {
                if (account.getBalance() < t.getAmount())
                    throw new RuntimeException("the balance of account " + account.getAddress() + " is not enough");
                account.setBalance(account.getBalance() - t.getAmount());
            }
            if (account.getAddress().equals(contractAccount.getCreatedBy())) {
                account.setBalance(account.getBalance() + t.getAmount());
                if (account.getBalance() < 0)
                    throw new RuntimeException("math overflow");
            }
            if (!account.getAddress().equals(t.getFromAddress()) && !account.getAddress().equals(t.getTo())
                    && !account.getAddress().equals(contractAccount.getCreatedBy()))
                throw new RuntimeException(
                        "unexpected address " + account.getAddress() + " not a createdBy or from or to");
        }

        if (preBuiltContractAddresses.containsKey(contractAccount.getAddress())) {
            Trie<byte[], byte[]> before = storageTrie.revert(contractAccount.getStorageRoot(),
                    new CachedStore<>(storageTrie.getStore(), ByteArrayMap::new));
            PreBuiltContract updater = preBuiltContractAddresses.get(contractAccount.getAddress());
            updater.update(header, t, accounts, before);
            byte[] root = before.commit();
            before.flush();
            contractAccount.setStorageRoot(root);
            return accounts;
        }

        // build Parameters here
        Context context = new Context(header, t, contractAccount, null);

        ContractDB contractDB = new ContractDB(storageTrie.revert(contractAccount.getStorageRoot(),
                new CachedStore<>(storageTrie.getStore(), ByteArrayMap::new)));

        Hosts hosts = new Hosts().withContext(context).withDB(contractDB);

        // every contract should have a init method
        ModuleInstance instance = ModuleInstance.builder().hooks(Collections.singleton(new GasLimit()))
                .hostFunctions(hosts.getAll())
                .binary(contractStore.get(contractAccount.getContractHash())
                        .orElseThrow(() -> new RuntimeException(
                                "contract " + HexBytes.encode(contractAccount.getContractHash()) + " not found in db")))
                .build();

        instance.execute(context.getMethod());
        contractDB.getStorageTrie().commit();
        contractDB.getStorageTrie().flush();
        contractAccount.setStorageRoot(contractDB.getStorageTrie().getRootHash());
        return accounts;
    }

    private void require(boolean b, String msg) throws RuntimeException {
        if (!b) {
            throw new RuntimeException(msg);
        }
    }
}

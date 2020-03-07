package org.tdf.sunflower.state;

import lombok.AllArgsConstructor;
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

/**
 * where state transition happens
 */
@AllArgsConstructor
@Slf4j(topic = "account")
public class AccountUpdater extends AbstractStateUpdater<HexBytes, Account> {
    @Getter
    private Map<HexBytes, Account> genesisStates;

    @Qualifier("contractCodeStore")
    private Store<byte[], byte[]> contractStore;

    @Qualifier("contractStorageTrie")
    private Trie<byte[], byte[]> storageTrie;


    @Override
    public Set<HexBytes> getRelatedKeys(Transaction transaction, Map<HexBytes, Account> store) {
        switch (Transaction.Type.TYPE_MAP.get(transaction.getType())) {
            case COIN_BASE:
                return Collections.singleton(transaction.getTo());
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
                    throw new RuntimeException("the nonce of transaction should be "
                            + (state.getNonce() + 1) + " while "
                            + transaction.getNonce() + " received");
                state.setNonce(state.getNonce() + 1);
            }
        }
        switch (Transaction.Type.TYPE_MAP.get(transaction.getType())) {
            case TRANSFER:
                return updateTransfer(cloned, transaction);
            case COIN_BASE:
                return updateCoinBase(cloned, transaction);
            case CONTRACT_DEPLOY:
                return updateDeploy(cloned, header, transaction);
            case CONTRACT_CALL:
                return updateTransactionCall(cloned, header, transaction);
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
        return new Account(address, 0, 0, HexBytes.EMPTY, new byte[0], storageTrie.getNullHash(), true);
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
                require(Long.compareUnsigned(state.getBalance(), t.getAmount()) >= 0, "the balance of sender is not enough");
                state.setBalance(state.getBalance() - t.getAmount());
                log.info("transfer from " + t.getFromAddress() + " to " + t.getTo() + " amount = " + t.getAmount());
            }
            if (t.getTo().equals(state.getAddress())) {
                state.setBalance(state.getBalance() + t.getAmount());
            }
            if (!t.getFromAddress().equals(state.getAddress())
                    && !t.getTo().equals(state.getAddress())
            )
                throw new RuntimeException(
                        "unreachable: nor to or from " + t.getHash() + " equals to the account " + state.getAddress()
                );
        }
        return states;
    }

    private Map<HexBytes, Account> updateCoinBase(Map<HexBytes, Account> accounts, Transaction t) {
        for (Account account : accounts.values()) {
            if (!account.getAddress().equals(t.getTo()))
                throw new RuntimeException("unreachable " + account + " is not coinbase of " + t.getHash());
            account.setBalance(account.getBalance() + t.getAmount());
        }
        return accounts;
    }


    private Map<HexBytes, Account> updateDeploy(Map<HexBytes, Account> accounts, Header header, Transaction t) {
        Account contractAccount = Objects.requireNonNull(accounts.get(t.createContractAddress()));

        contractAccount.setCreatedBy(t.getFromAddress());
        contractAccount.setNonce(t.getNonce());

        // build Parameters here
        Context context = new Context(header, t, contractAccount, null);

        ContractDB contractDB = new ContractDB(
                storageTrie.revert(
                        storageTrie.getNullHash(),
                        new CachedStore<>(storageTrie.getStore(), ByteArrayMap::new)
                )
        );

        Hosts hosts = new Hosts()
                .withContext(context)
                .withDB(contractDB);

        // every contract must has a init method
        ModuleInstance instance = ModuleInstance.builder()
                .hooks(Collections.singleton(new GasLimit()))
                .binary(t.getPayload().getBytes())
                .hostFunctions(hosts.getAll())
                .build();

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

    private Map<HexBytes, Account> updateTransactionCall(Map<HexBytes, Account> accounts, Header header, Transaction t) {
        Account contractAccount = accounts.get(t.getTo());
        for (Account account : accounts.values()) {
            if (account.getAddress().equals(t.getFromAddress())) {
                if (account.getBalance() < t.getAmount())
                    throw new RuntimeException("the balance of account " + account.getAddress() + " is not enough");
                account.setBalance(account.getBalance() - t.getAmount());
            }
            if (account.getAddress().equals(contractAccount.getCreatedBy())) {
                account.setBalance(account.getBalance() + t.getAmount());
            }
            if (!account.getAddress().equals(t.getFromAddress()) &&
                    !account.getAddress().equals(t.getTo()) &&
                    !account.getAddress().equals(contractAccount.getCreatedBy())
            )
                throw new RuntimeException("unexpected address " + account.getAddress() + " not a createdBy or from or to");
        }

        // build Parameters here
        Context context = new Context(header, t, contractAccount, null);

        ContractDB contractDB = new ContractDB(
                storageTrie.revert(
                        contractAccount.getStorageRoot(),
                        new CachedStore<>(storageTrie.getStore(), ByteArrayMap::new)
                )
        );

        Hosts hosts = new Hosts()
                .withContext(context)
                .withDB(contractDB);

        // every contract should have a init method
        ModuleInstance instance = ModuleInstance.builder()
                .hooks(Collections.singleton(new GasLimit()))
                .hostFunctions(hosts.getAll())
                .binary(
                        contractStore
                                .get(contractAccount.getContractHash())
                                .orElseThrow(
                                        () ->
                                                new RuntimeException("contract " + HexBytes.encode(contractAccount.getContractHash()) + " not found in db")
                                )
                )
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

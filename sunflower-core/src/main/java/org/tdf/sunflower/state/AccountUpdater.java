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
import org.tdf.sunflower.facade.BasicMessageQueue;
import org.tdf.sunflower.types.CryptoContext;
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

    private final BasicMessageQueue messageQueue;

    public AccountUpdater(Map<HexBytes, Account> genesisStates, Store<byte[], byte[]> contractStore,
                          Trie<byte[], byte[]> storageTrie, List<PreBuiltContract> preBuiltContracts,
                          List<Bios> biosList, BasicMessageQueue messageQueue
    ) {
        this.genesisStates = new HashMap<>(genesisStates);
        this.contractStore = contractStore;
        this.storageTrie = storageTrie;
        this.preBuiltContracts = preBuiltContracts;
        this.biosList = biosList.stream().collect(
                Collectors.toMap(x -> x.getGenesisAccount().getAddress(), Function.identity())
        );
        this.messageQueue = messageQueue;
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
    public void update(Map<HexBytes, Account> states, Header header, Transaction tx) {
        if (tx.getType() == Transaction.Type.COIN_BASE.code && header.getHeight() != tx.getNonce()) {
            throw new RuntimeException("nonce of coinbase transaction should be " + header.getHeight());
        }
        createEmptyAccounts(states, Collections.singleton(Constants.FEE_ACCOUNT_ADDR));
        switch (Transaction.Type.TYPE_MAP.get(tx.getType())) {
            case TRANSFER: {
                createEmptyAccounts(states, Collections.singletonList(tx.getTo()));
                increaseNonce(states, tx);
                updateTransfer(
                        states,
                        tx.getFromAddress(),
                        tx.getTo(),
                        tx.getAmount(),
                        tx.getFee()
                );
                return;
            }
            case COIN_BASE: {
                createEmptyAccounts(states, Collections.singletonList(tx.getTo()));
                updateCoinBase(states, header, tx);
                return;
            }
            case CONTRACT_DEPLOY:
                createEmptyAccounts(
                        states,
                        Arrays.asList(tx.getFromAddress(), tx.createContractAddress())
                );
                increaseNonce(states, tx);
                updateDeploy(states, header, tx);
                return;
            case CONTRACT_CALL:
                createEmptyAccounts(
                        states,
                        Collections.singletonList(tx.getFromAddress())
                );
                increaseNonce(states, tx);
                updateContractCall(states, header, tx);
                return;
        }
        throw new RuntimeException("unknown type " + tx.getType());
    }

    public static void updateFeeAccount(Map<HexBytes, Account> states, long fee) {
        Account feeAccount = states.get(Constants.FEE_ACCOUNT_ADDR);
        feeAccount.setBalance(SafeMath.add(feeAccount.getBalance(), fee));
        states.put(feeAccount.getAddress(), feeAccount);
    }

    public void createEmptyAccounts(Map<HexBytes, Account> states, Collection<? extends HexBytes> addresses) {
        for (HexBytes addr : addresses) {
            states.putIfAbsent(addr, createEmpty(addr));
        }
    }

    public static void increaseNonce(Map<HexBytes, Account> states, Transaction tx) {
        Account state = states.get(tx.getFromAddress());
        if (state.getNonce() + 1 != tx.getNonce())
            throw new RuntimeException("the nonce of transaction should be " + (state.getNonce() + 1)
                    + " while " + tx.getNonce() + " received");
        state.setNonce(state.getNonce() + 1);
        states.put(state.getAddress(), state);
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


    public static void updateTransfer(Map<HexBytes, Account> states, HexBytes fromAddr, HexBytes toAddr, long amount, long fee) {
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
        updateFeeAccount(states, fee);
    }

    private void updateCoinBase(Map<HexBytes, Account> accounts, Header header, Transaction t) {
        Account to = accounts.get(t.getTo());
        to.setBalance(SafeMath.add(to.getBalance(), t.getAmount()));
        accounts.put(to.getAddress(), to);

        for (Bios bios : biosList.values()) {
            Account a = accounts.get(bios.getGenesisAccount().getAddress());
            Trie<byte[], byte[]> before = storageTrie.revert(a.getStorageRoot(),
                    new CachedStore<>(storageTrie.getStore(), ByteArrayMap::new));
            bios.update(header, before);
            byte[] root = before.commit();
            before.flush();
            a.setStorageRoot(root);
            accounts.put(a.getAddress(), a);
        }
    }

    private void updateDeploy(Map<HexBytes, Account> accounts, Header header, Transaction t) {
        Account contractAccount = Objects.requireNonNull(accounts.get(t.createContractAddress()));
        Account createdBy = accounts.get(t.getFromAddress());

        contractAccount.setCreatedBy(t.getFromAddress());
        contractAccount.setNonce(t.getNonce());

        // build Parameters here
        Context context = new Context(header, t, contractAccount, null);

        ContractDB contractDB = new ContractDB(storageTrie.revert(storageTrie.getNullHash(),
                new CachedStore<>(storageTrie.getStore(), ByteArrayMap::new)));

        Hosts hosts = new Hosts()
                .withTransfer(accounts, t, t.getFromAddress(), t.createContractAddress())
                .withContext(context)
                .withDB(contractDB)
                .withEvent(messageQueue, t.createContractAddress());

        GasLimit gasLimit = new GasLimit();
        // every contract must has a init method
        ModuleInstance instance = ModuleInstance.builder().hooks(Collections.singleton(gasLimit))
                .binary(t.getPayload().getBytes()).hostFunctions(hosts.getAll()).build();

        byte[] contractHash = CryptoContext.hash(t.getPayload().getBytes());
        contractStore.put(contractHash, t.getPayload().getBytes());
        contractAccount.setContractHash(contractHash);
        contractAccount.setBalance(t.getAmount());


        if (instance.containsExport("init")) {
            instance.execute("init");
        }

        long fee = ((t.getPayload().size() / 1024) + gasLimit.getGas()) * t.getGasPrice();
        createdBy.setBalance(SafeMath.sub(createdBy.getBalance(), SafeMath.add(t.getAmount(), fee)));
        contractDB.getStorageTrie().commit();
        contractDB.getStorageTrie().flush();
        contractAccount.setStorageRoot(contractDB.getStorageTrie().getRootHash());

        accounts.put(contractAccount.getAddress(), contractAccount);
        accounts.put(createdBy.getAddress(), createdBy);
        updateFeeAccount(accounts, fee);
        log.info("deploy contract at " + contractAccount.getAddress() + " success");
    }

    private void updateContractCall(Map<HexBytes, Account> accounts, Header header, Transaction t) {
        Account contractAccount = accounts.get(t.getTo());
        Account caller = accounts.get(t.getFromAddress());

        contractAccount.setBalance(SafeMath.add(contractAccount.getBalance(), t.getAmount()));

        if (preBuiltContractAddresses.containsKey(contractAccount.getAddress())) {
            long fee = 10 * t.getGasPrice();
            caller.setBalance(SafeMath.sub(caller.getBalance(), SafeMath.add(fee, t.getAmount())));
            Trie<byte[], byte[]> before = storageTrie.revert(contractAccount.getStorageRoot(),
                    new CachedStore<>(storageTrie.getStore(), ByteArrayMap::new));
            PreBuiltContract updater = preBuiltContractAddresses.get(contractAccount.getAddress());
            updater.update(header, t, accounts, before);
            byte[] root = before.commit();
            before.flush();
            contractAccount.setStorageRoot(root);
            updateFeeAccount(accounts, fee);
            return;
        }

        // build Parameters here
        Context context = new Context(header, t, contractAccount, t.getPayload());

        ContractDB contractDB = new ContractDB(storageTrie.revert(contractAccount.getStorageRoot(),
                new CachedStore<>(storageTrie.getStore(), ByteArrayMap::new)));

        Hosts hosts = new Hosts()
                .withTransfer(accounts, t, contractAccount.getCreatedBy(), contractAccount.getAddress())
                .withContext(context)
                .withDB(contractDB)
                .withEvent(messageQueue, contractAccount.getAddress());

        GasLimit limit = new GasLimit();
        // every contract should have a init method
        ModuleInstance instance = ModuleInstance.builder().hooks(Collections.singleton(limit))
                .hostFunctions(hosts.getAll())
                .binary(contractStore.get(contractAccount.getContractHash())
                        .orElseThrow(() -> new RuntimeException(
                                "contract " + HexBytes.encode(contractAccount.getContractHash()) + " not found in db")))
                .build();

        instance.execute(context.getMethod());
        long fee = limit.getGas() * t.getGasPrice();
        caller.setBalance(SafeMath.sub(caller.getBalance(), SafeMath.add(fee, t.getAmount())));
        contractDB.getStorageTrie().commit();
        contractDB.getStorageTrie().flush();
        contractAccount.setStorageRoot(contractDB.getStorageTrie().getRootHash());

        accounts.put(contractAccount.getAddress(), contractAccount);
        accounts.put(caller.getAddress(), caller);
        updateFeeAccount(accounts, fee);
    }

    private static void require(boolean b, String msg) throws RuntimeException {
        if (!b) {
            throw new RuntimeException(msg);
        }
    }
}

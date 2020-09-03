package org.tdf.sunflower.state;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.tdf.common.store.CachedStore;
import org.tdf.common.store.Store;
import org.tdf.common.trie.Trie;
import org.tdf.common.util.BigEndian;
import org.tdf.common.util.ByteArrayMap;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.facade.BasicMessageQueue;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.types.Transaction;
import org.tdf.sunflower.vm.abi.Context;
import org.tdf.sunflower.vm.abi.ContractCall;
import org.tdf.sunflower.vm.hosts.Limit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    public static void updateFeeAccount(Map<HexBytes, Account> states, long fee) {
        Account feeAccount = states.get(Constants.FEE_ACCOUNT_ADDR);
        feeAccount.setBalance(SafeMath.add(feeAccount.getBalance(), fee));
        states.put(feeAccount.getAddress(), feeAccount);
    }

    public static void increaseNonce(Map<HexBytes, Account> states, Transaction tx) {
        Account state = states.get(tx.getFromAddress());
        if (state.getNonce() + 1 != tx.getNonce())
            throw new RuntimeException("the nonce of transaction should be " + (state.getNonce() + 1)
                    + " while " + tx.getNonce() + " received");
        state.setNonce(tx.getNonce());
        states.put(state.getAddress(), state);
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

    private static void require(boolean b, String msg) throws RuntimeException {
        if (!b) {
            throw new RuntimeException(msg);
        }
    }

    @Override
    public void update(Map<HexBytes, Account> states, Header header, Transaction tx) {
        if (tx.getType() == Transaction.Type.COIN_BASE.code && header.getHeight() != tx.getNonce()) {
            throw new RuntimeException("nonce of coinbase transaction should be " + header.getHeight());
        }
        states.putIfAbsent(Constants.FEE_ACCOUNT_ADDR, Account.emptyAccount(Constants.FEE_ACCOUNT_ADDR));
        switch (Transaction.Type.TYPE_MAP.get(tx.getType())) {
            case TRANSFER: {
                states.putIfAbsent(tx.getTo(), Account.emptyAccount(tx.getTo()));
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
                states.putIfAbsent(tx.getTo(), Account.emptyAccount(tx.getTo()));
                updateCoinBase(states, header, tx);
                return;
            }
            case CONTRACT_DEPLOY:
                updateDeploy(states, header, tx);
                return;
            case CONTRACT_CALL:
                states.putIfAbsent(tx.getFromAddress(), Account.emptyAccount(tx.getFromAddress()));
                increaseNonce(states, tx);
                updateContractCall(states, header, tx);
                return;
        }
        throw new RuntimeException("unknown type " + tx.getType());
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


        int binaryLen = BigEndian.decodeInt32(t.getPayload().slice(0, 4).getBytes());
        byte[] binary = t.getPayload().slice(4, 4 + binaryLen).getBytes();
        byte[] parameters = t.getPayload().slice(4 + binaryLen).getBytes();


        Limit limit = new Limit();


        // execute constructor of contract
        ContractCall contractCall = new ContractCall(
                accounts, header,
                t, storageTrie,
                messageQueue, contractStore,
                limit, 0, t.getFromAddress()
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
        updateFeeAccount(accounts, fee);
        log.info("deploy contract at " + contractAccount.getAddress() + " success");
    }


    private void updateContractCall(Map<HexBytes, Account> accounts, Header header, Transaction t) {
        Account contractAccount = accounts.get(t.getTo());

        if (preBuiltContractAddresses.containsKey(contractAccount.getAddress())) {
            long fee = SafeMath.mul(10, t.getGasPrice());
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

        // execute method
        Limit limit = new Limit();
        ContractCall contractCall = new ContractCall(
                accounts, header,
                t, storageTrie,
                messageQueue, contractStore,
                limit, 0, t.getFromAddress()
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
        updateFeeAccount(accounts, fee);
    }
}

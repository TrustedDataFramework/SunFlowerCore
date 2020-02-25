package org.tdf.sunflower.state;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.tdf.common.util.HexBytes;
import org.tdf.lotusvm.ModuleInstance;
import org.tdf.sunflower.account.Address;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.types.Transaction;
import org.tdf.sunflower.vm.abi.Context;
import org.tdf.sunflower.vm.hosts.Hosts;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@AllArgsConstructor
public class AccountUpdater extends AbstractStateUpdater<HexBytes, Account> {
    @Getter
    private Map<HexBytes, Account> genesisStates;

    @Override
    public Set<HexBytes> getRelatedKeys(Transaction transaction, Map<HexBytes, Account> store) {
        switch (Transaction.Type.TYPE_MAP.get(transaction.getType())) {
            case COIN_BASE:
                return Collections.singleton(transaction.getTo());
            case TRANSFER:
                return Stream
                        .of(transaction.getFrom(), transaction.getTo())
                        .filter(x -> !x.isEmpty())
                        .collect(Collectors.toSet());
            case CONTRACT_DEPLOY: {
                return Collections.singleton(transaction.createContractAddress());
            }
            case CONTRACT_CALL: {
                Set<HexBytes> ret = new HashSet<>();
                ret.add(transaction.getFrom());
                ret.add(transaction.getTo());
                ret.add(Objects.requireNonNull(store.get(transaction.getTo())).getCreatedBy());
                return ret;
            }
        }
        throw new RuntimeException("unreachable");
    }

    @Override
    public Map<HexBytes, Account> update(Map<HexBytes, Account> states, Header header, Transaction transaction) {
        Map<HexBytes, Account> cloned = createEmptyMap();
        states.forEach((k, v) -> cloned.put(k, v.clone()));
        switch (Transaction.Type.TYPE_MAP.get(transaction.getType())) {
            case TRANSFER:
                return updateTransfer(cloned, transaction);
            case COIN_BASE:
                return updateCoinBase(cloned, transaction);
            case CONTRACT_DEPLOY:
                return updateDeploy(cloned, header, transaction);
            case CONTRACT_CALL:
                return updateAnother(cloned, header, transaction);
        }
        throw new RuntimeException("unknown type " + transaction.getType());
    }

    @Override
    public Account createEmpty(HexBytes address) {
        return new Account(address, 0);
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
            if (Address.fromPublicKey(t.getFrom().getBytes()).equals(state.getAddress())) {
                require(Long.compareUnsigned(state.getBalance(), t.getAmount()) >= 0, "the balance of sender is not enough");
                state.setBalance(state.getBalance() - t.getAmount());
            }
            if (t.getTo().equals(state.getAddress())) {
                state.setBalance(state.getBalance() + t.getAmount());
            }
            if (!t.getFrom().equals(state.getAddress()) && !t.getTo().equals(state.getAddress()))
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
        Account contractAccount = accounts.values().stream().findAny().orElse(null);
        if (accounts.size() != 1 || !contractAccount.getAddress().equals(t.createContractAddress())
        )
            throw new RuntimeException("unreachable: contract deploy " + t.getHash() + " related to only one address");
        contractAccount.setCreatedBy(t.getFrom());
        contractAccount.setNonce(t.getNonce());
        contractAccount.setBinaryContract(t.getPayload().getBytes());
        // build Parameters here
        Context context = Context.fromTransaction(header, t);

        Hosts hosts = new Hosts()
                .withContext(context)
                .withPayload(context.getPayload());

        // every contract must has a init method
        ModuleInstance instance = ModuleInstance.builder()
                .binary(t.getPayload().getBytes())
                .hostFunctions(hosts.getAll())
                .build();

        if(instance.containsExport("init")){
           instance.execute("init");
        }
        contractAccount.setMemory(instance.getMemory());
        contractAccount.setGlobals(instance.getGlobals());
        return accounts;
    }

    private Map<HexBytes, Account> updateAnother(Map<HexBytes, Account> accounts, Header header, Transaction t) {
        Account contractAccount = accounts.get(t.getTo());
        for (Account account : accounts.values()) {
            if(account.getAddress().equals(t.getFrom())){
                if(account.getBalance() < t.getAmount())
                    throw new RuntimeException("the balance of account " + account.getAddress() + " is not enough");
                account.setBalance(account.getBalance() - t.getAmount());
            }
            if(account.getAddress().equals(contractAccount.getCreatedBy())){
                account.setBalance(account.getBalance() + t.getAmount());
            }
            if(!account.getAddress().equals(t.getFrom()) &&
                    !account.getAddress().equals(t.getTo()) &&
                    !account.getAddress().equals(contractAccount.getCreatedBy())
            ) throw new RuntimeException("unexpected address " + account.getAddress() + " not a createdBy or from or to");
        }

        // build Parameters here
        Context context = Context.fromTransaction(header, t);

        Hosts hosts = new Hosts()
                .withContext(context)
                .withPayload(context.getPayload());

        // every contract must has a init method
        ModuleInstance instance = ModuleInstance.builder()
                .hostFunctions(hosts.getAll())
                .memory(contractAccount.getMemory())
                .globals(contractAccount.getGlobals())
                .build();

        instance.execute(context.getMethod());
        contractAccount.setMemory(instance.getMemory());
        contractAccount.setGlobals(instance.getGlobals());
        return accounts;
    }

    private void require(boolean b, String msg) throws RuntimeException {
        if (!b) {
            throw new RuntimeException(msg);
        }
    }


}

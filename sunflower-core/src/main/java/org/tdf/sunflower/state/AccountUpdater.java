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
    public Set<HexBytes> getRelatedKeys(Transaction transaction) {
        return Stream
                .of(transaction.getFrom(), transaction.getTo())
                .filter(x -> !x.isEmpty())
                .collect(Collectors.toSet());
    }

    @Override
    public Account update(HexBytes hexBytes, Account state, Header header, Transaction transaction) {
        switch (Transaction.Type.TYPE_MAP.get(transaction.getType())) {
            case TRANSFER:
                return updateTransfer(state, transaction);
            case COIN_BASE:
                return updateCoinBase(state, transaction);
            default:
                return updateAnother(state, header, transaction);
        }
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

    private Account updateTransfer(Account state, Transaction t) {
        if (Address.fromPublicKey(t.getFrom().getBytes()).equals(state.getAddress())) {
            require(Long.compareUnsigned(state.getBalance(), t.getAmount()) >= 0, "the balance of sender is not enough");
            state.setBalance(state.getBalance() - t.getAmount());
        }
        if (t.getTo().equals(state.getAddress())) {
            state.setBalance(state.getBalance() + t.getAmount());
        }
        return state;
    }

    private Account updateCoinBase(Account account, Transaction t) {
        if (account.getAddress().equals(t.getTo())) {
            account.setBalance(account.getBalance() + t.getAmount());
        }
        return account;
    }

    private Account updateAnother(Account account, Header header, Transaction t) {
        if (account.getAddress().equals(
                Address.fromPublicKey(t.getFrom().getBytes())
        )) {
            account.setBalance(account.getBalance() - t.getAmount());
        }
        if (!t.getTo().equals(account.getAddress())) {
            return account;
        }
        if (t.getType() == Transaction.Type.CONTRACT_CALL.code) {
            require(account.containsContract(), t.getHash().toString()
                    + " call a contract " + account.getAddress() + " without deploy");
        }
        if (t.getType() == Transaction.Type.CONTRACT_DEPLOY.code) {
            require(account.containsContract(), t.getHash().toString()
                    + " deploy a contract on " + account.getAddress() + " contains a deployed contract");
        }
        // build Parameters here
        Context context = Context.fromTransaction(header, t);

        Hosts hosts = new Hosts()
                .withContext(context)
                .withPayload(context.getPayload());

        // every contract must has a init method
        ModuleInstance.Builder builder = ModuleInstance.builder()
                .hostFunctions(hosts.getAll());

        if (t.getType() != Transaction.Type.CONTRACT_DEPLOY.code) {
            // cannot call init method if the contract had been deployed
            require(!context.getMethod().equals("init"), t.getHash().toString()
                    + " call init method of deployed contract on " + account.getAddress());

            builder = builder.memory(account.getMemory())
                    .binary(account.getBinaryContract())
                    .globals(account.getGlobals())
                    .initMemory(false)
                    .initGlobals(false);
        } else {
            account.setBinaryContract(t.payload.getBytes());
            builder = builder.binary(t.payload.getBytes());
        }
        try {
            ModuleInstance instance = builder.build();
            if (!instance.hasExport(context.getMethod())) {
                throw new RuntimeException("contract not has method " + context.getMethod());
            }
            instance.execute(context.getMethod());
            account.setMemory(instance.getMemory());
            account.setGlobals(instance.getGlobals());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(t.getHash().toString()
                    + " deploy contract " + account.getAddress() + " failed");
        }
        return account;
    }

    private void require(boolean b, String msg) throws RuntimeException {
        if (!b) {
            throw new RuntimeException(msg);
        }
    }


}

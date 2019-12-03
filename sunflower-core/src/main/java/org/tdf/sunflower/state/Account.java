package org.tdf.sunflower.state;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.tdf.common.Block;
import org.tdf.common.ForkAbleState;
import org.tdf.common.Header;
import org.tdf.common.Transaction;
import org.tdf.exception.StateUpdateException;
import org.tdf.lotusvm.runtime.ModuleInstance;
import org.tdf.sunflower.account.PublicKeyHash;
import org.tdf.sunflower.vm.abi.Context;
import org.tdf.sunflower.vm.hosts.Hosts;
import org.wisdom.crypto.ed25519.Ed25519;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Account implements ForkAbleState<Account> {
    @Getter
    private PublicKeyHash publicKeyHash;
    @Getter
    private long balance;
    // if the account contains none contract, binary contract will be null
    @Getter
    private byte[] binaryContract;
    @Getter
    private byte[] memory;
    @Getter
    private long[] globals;

    // create a random account
    public static Account getRandomAccount() {
        return builder().publicKeyHash(
                PublicKeyHash.fromPublicKey(Ed25519.generateKeyPair().getPublicKey().getEncoded())
        ).build();
    }

    public Account(PublicKeyHash publicKeyHash, long balance) {
        this.publicKeyHash = publicKeyHash;
        this.balance = balance;
    }

    public Account(String address) {
        this.publicKeyHash = PublicKeyHash.from(address).orElseThrow(() -> new RuntimeException("invalid address " + address));
    }

    @Override
    public String getIdentifier() {
        return publicKeyHash.getAddress();
    }

    @Override
    public Set<String> getIdentifiersOf(Transaction transaction) {
        return Stream.of(
                PublicKeyHash.fromPublicKey(transaction.getFrom().getBytes()).getAddress(),
                new PublicKeyHash(transaction.getTo().getBytes()).getAddress()
        ).collect(Collectors.toSet());
    }

    @Override
    public Account createEmpty(String id) {
        return new Account(
                PublicKeyHash.from(id).orElseThrow(() -> new RuntimeException("invalid hex string " + id)), 0
        );
    }

    private void require(boolean b, String msg) throws StateUpdateException {
        if (!b) {
            throw new StateUpdateException(msg);
        }
    }

    private void updateTransfer(Block b, Transaction t) throws StateUpdateException {
        if (publicKeyHash.equals(
                PublicKeyHash.fromPublicKey(t.getFrom().getBytes())
        )) {
            require(Long.compareUnsigned(balance, t.getAmount()) >= 0, "the balance of sender is not enough");
            balance -= t.getAmount();
        }
        if (publicKeyHash.equals(new PublicKeyHash(t.getTo().getBytes()))) {
            balance += t.getAmount();
        }
    }

    private void updateCoinBase(Transaction t) {
        if (publicKeyHash.equals(new PublicKeyHash(t.getTo().getBytes()))) {
            balance += t.getAmount();
        }
    }

    @Override
    public void update(Block b, Transaction t) throws StateUpdateException {
        PublicKeyHash to = new PublicKeyHash(t.getTo().getBytes());
        if (t.getType() == Transaction.Type.COIN_BASE.code) {
            updateCoinBase(t);
            return;
        }
        if (t.getType() == Transaction.Type.TRANSFER.code) {
            updateTransfer(b, t);
            return;
        }
        if (publicKeyHash.equals(PublicKeyHash.fromPublicKey(t.getFrom().getBytes()))) {
            balance -= t.getAmount();
        }
        if (!to.equals(publicKeyHash)) {
            return;
        }
        if (t.getType() == Transaction.Type.CONTRACT_CALL.code) {
            require(this.binaryContract != null, t.getHash().toString()
                    + " call a contract " + publicKeyHash.getAddress() + " without deploy");
        }
        if (t.getType() == Transaction.Type.CONTRACT_DEPLOY.code) {
            require(this.binaryContract == null, t.getHash().toString()
                    + " deploy a contract on " + publicKeyHash.getAddress() + " contains a deployed contract");
        }
        // build Parameters here
        Context context = Context.fromTransaction(b, t);

        Hosts hosts = new Hosts()
                .withContext(context)
                .withPayload(context.getPayload());

        // every contract must has a init method
        ModuleInstance.Config.ConfigBuilder builder = ModuleInstance.Config.builder()
                .hostFunctions(hosts.getAll());

        if (t.getType() != Transaction.Type.CONTRACT_DEPLOY.code) {
            // cannot call init method if the contract had been deployed
            require(!context.getMethod().equals("init"), t.getHash().toString()
                    + " call init method of deployed contract on " + publicKeyHash.getAddress());
            builder = builder.memory(memory)
                    .binary(binaryContract)
                    .globals(globals)
                    .initMemory(false)
                    .initGlobals(false);
        } else {
            this.binaryContract = t.payload.getBytes();
            builder = builder.binary(t.payload.getBytes());
        }
        try {
            ModuleInstance instance = new ModuleInstance(
                    builder.build()
            );
            if (!instance.getExports().containsKey(context.getMethod())) {
                throw new RuntimeException("contract not has method " + context.getMethod());
            }
            instance.execute(context.getMethod());
            memory = instance.getMemory().getData();
            globals = instance.getGlobals().getData();
            require(Long.compareUnsigned(balance, instance.getGas() * t.getGasPrice()) >= 0,
                    "the balance is not enough to pay for fee");
        } catch (Exception e) {
            e.printStackTrace();
            throw new StateUpdateException(t.getHash().toString()
                    + " deploy contract " + publicKeyHash.getAddress() + " failed");
        }
    }

    @Override
    public void update(Header header) throws StateUpdateException {

    }

    public byte[] view(String method, byte[] parameters) throws Exception {
        Hosts hosts = new Hosts().withPayload(parameters);
        ModuleInstance.Config.ConfigBuilder builder = ModuleInstance.Config.builder()
                .memory(memory)
                .globals(globals)
                .initMemory(false)
                .initGlobals(false)
                .hostFunctions(new Hosts().withPayload(parameters).getAll());
        ModuleInstance instance = new ModuleInstance(
                builder.hostFunctions(hosts.getAll()).build()
        );
        instance.execute(method);
        return hosts.getResult();
    }

    @Override
    public Account clone() {
        return new Account(publicKeyHash, balance, binaryContract,
                memory == null ? null : Arrays.copyOfRange(memory, 0, memory.length),
                globals == null ? null : Arrays.copyOfRange(globals, 0, globals.length)
        );
    }
}

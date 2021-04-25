package org.tdf.sunflower.pool;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.tdf.common.trie.Trie;
import org.tdf.common.types.Uint256;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.Bios;
import org.tdf.sunflower.state.PreBuiltContract;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.vm.Backend;
import org.tdf.sunflower.vm.abi.ContractABI;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// backend for mining
@AllArgsConstructor
@NoArgsConstructor
public class MinerBackend implements Backend {
    private Block parent;
    private MinerBackend parentBackend;

    private Trie<HexBytes, Account> trie;
    private Trie<byte[], byte[]> contractStorageTrie;

    private Map<HexBytes, Account> modifiedAccounts;
    private Map<HexBytes, Map<HexBytes, HexBytes>> modifiedStorage;
    private List<PreBuiltContract> preBuiltContracts;
    private Map<HexBytes, Bios> bios;

    // get account by parent without clone
    private Account getAccount(HexBytes address) {
        if (parentBackend != null)
            return parentBackend.getAccount(address);
        return trie.get(address).orElse(Account.emptyAccount(address));
    }

    public MinerBackend createChild() {
        return new MinerBackend(
                parent,
                this,
                trie,
                contractStorageTrie,
                new HashMap<>(),
                new HashMap<>(),
                preBuiltContracts,
                bios
        );
    }

    @Override
    public long getHeight() {
        return parent.getHeight() + 1;
    }

    @Override
    public HexBytes getParentHash() {
        return parent.getHash();
    }

    @Override
    public Uint256 getBalance(HexBytes address) {
        if (modifiedAccounts.containsKey(address)) {
            return modifiedAccounts.get(address).getBalance();
        }
        if (parentBackend != null)
            return parentBackend.getBalance(address);
        return trie
                .get(address)
                .map(Account::getBalance)
                .orElse(Uint256.ZERO);
    }

    @Override
    public long getHeaderCreatedAt() {
        return 0;
    }

    @Override
    public void setBalance(HexBytes address, Uint256 balance) {
        if (modifiedAccounts.containsKey(address)) {
            modifiedAccounts.get(address).setBalance(balance);
            return;
        }
        Account a = getAccount(address).clone();
        a.setBalance(balance);
        modifiedAccounts.put(address, a);
    }

    @Override
    public long getNonce(HexBytes address) {
        if (modifiedAccounts.containsKey(address)) {
            return modifiedAccounts.get(address).getNonce();
        }
        if (parentBackend != null){
            return parentBackend.getNonce(address);
        }
        return trie.get(address).map(Account::getNonce).orElse(0L);
    }

    @Override
    public void setNonce(HexBytes address, long nonce) {
        if (modifiedAccounts.containsKey(address)) {
            modifiedAccounts.get(address).setNonce(nonce);
            return;
        }
        Account a = getAccount(address).clone();
        a.setNonce(nonce);
        modifiedAccounts.put(address, a);
    }

    @Override
    public List<ContractABI> getABI(HexBytes address) {
        if (modifiedAccounts.containsKey(address)) {
            return modifiedAccounts.get(address).getContractABIs();
        }
        if (parentBackend != null){
            return parentBackend.getABI(address);
        }
        return trie.get(address).map(Account::getContractABIs).orElse(Collections.emptyList());
    }

    @Override
    public void setABI(HexBytes address, List<ContractABI> abi) {
        if (modifiedAccounts.containsKey(address)) {
            modifiedAccounts.get(address).setContractABIs(abi);
            return;
        }
        Account a = getAccount(address).clone();
        a.setContractABIs(abi);
        modifiedAccounts.put(address, a);
    }

    @Override
    public long getGasLimit() {
        return 0;
    }

    @Override
    public long getInitialGas(int payloadSize) {
        return payloadSize / 1024;
    }

    @Override
    public long getMaxDepth() {
        return 64;
    }

    @Override
    public Map<HexBytes, PreBuiltContract> getPreBuiltContracts() {
        return null;
    }

    @Override
    public Map<HexBytes, Bios> getBios() {
        return null;
    }

    @Override
    public void dbSet(HexBytes address, byte[] key, byte[] value) {
        modifiedStorage.putIfAbsent(address , new HashMap<>());
        modifiedStorage.get(address).put(HexBytes.fromBytes(key), HexBytes.fromBytes(value));
    }

    @Override
    public byte[] dbGet(HexBytes address, byte[] key) {
        if (!modifiedStorage.containsKey(address)) {
            return parentBackend == null ?
                    trie.get(address)
                            .map(Account::getStorageRoot)
                            .map(contractStorageTrie::revert)
                            .flatMap(x -> x.get(key)).orElse(new byte[0]):
                    parentBackend.dbGet(address, key);
        }
    }

    @Override
    public boolean dbHas(HexBytes address, byte[] key) {
        return false;
    }

    @Override
    public HexBytes getContractCreatedBy(HexBytes address) {
        return null;
    }

    @Override
    public void setContractCreatedBy(HexBytes address, HexBytes createdBy) {

    }

    @Override
    public void dbRemove(HexBytes address, byte[] key) {

    }

    @Override
    public Uint256 getGasPrice() {
        return null;
    }

    @Override
    public byte[] getCode(HexBytes address) {
        return new byte[0];
    }

    @Override
    public byte[] setCode(HexBytes address, byte[] code) {
        return new byte[0];
    }

    @Override
    public void onEvent(HexBytes address, String eventName, byte[] eventData) {

    }
}

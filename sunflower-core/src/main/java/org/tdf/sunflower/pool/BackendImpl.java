package org.tdf.sunflower.pool;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.tdf.common.store.Store;
import org.tdf.common.trie.Trie;
import org.tdf.common.types.Uint256;
import org.tdf.common.util.HexBytes;
import org.tdf.rlp.RLPList;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.Bios;
import org.tdf.sunflower.state.PreBuiltContract;
import org.tdf.sunflower.types.CryptoContext;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.vm.Backend;
import org.tdf.sunflower.vm.ContractABI;

import java.util.*;

// backend for mining
@AllArgsConstructor
@NoArgsConstructor
public class BackendImpl implements Backend {
    private Header parent;
    @Getter
    private BackendImpl parentBackend;

    private Trie<HexBytes, Account> trie;
    private Trie<byte[], byte[]> contractStorageTrie;

    private Map<HexBytes, Account> modifiedAccounts;
    private Map<HexBytes, Map<HexBytes, HexBytes>> modifiedStorage;
    private Map<HexBytes, PreBuiltContract> preBuiltContracts;
    private Map<HexBytes, Bios> bios;
    private boolean isStatic;

    private Store<byte[], byte[]> codeStore;
    private Map<HexBytes, byte[]> codeCache;
    private Map<HexBytes, List<Map.Entry<String, RLPList>>> events;
    private long headerCreatedAt;

    // get account by parent without clone
    private Account lookup(HexBytes address) {
        if (parentBackend != null)
            return parentBackend.lookup(address);
        Account a = trie.get(address);
        if (a != null)
            return a;
        return Account.emptyAccount(address, Uint256.ZERO);
    }

    public BackendImpl createChild() {
        return new BackendImpl(
                parent,
                this,
                trie,
                contractStorageTrie,
                new HashMap<>(),
                new HashMap<>(),
                preBuiltContracts,
                bios,
                isStatic,
                codeStore,
                codeCache,
                events,
                headerCreatedAt
        );
    }

    private void mergeInternal(Map<HexBytes, Account> accounts, Map<HexBytes, Map<HexBytes, byte[]>> storage) {
        if (parentBackend != null) {
            parentBackend.mergeInternal(accounts, storage);
        }

        modifiedAccounts.forEach(accounts::put);
        for (Map.Entry<HexBytes, Map<HexBytes, byte[]>> entries : storage.entrySet()) {
            storage.putIfAbsent(entries.getKey(), new HashMap<>());
            storage.get(entries.getKey()).putAll(entries.getValue());
        }
    }

    @Override
    public byte[] merge() {
        Map<HexBytes, Account> accounts = new HashMap<>();
        Map<HexBytes, Map<HexBytes, byte[]>> storage = new HashMap<>();
        mergeInternal(accounts, storage);

        Set<HexBytes> modified = new HashSet<>();
        modified.addAll(accounts.keySet());
        modified.addAll(storage.keySet());

        Trie<HexBytes, Account> tmpTrie = trie.revert(parent.getStateRoot().getBytes());

        for (HexBytes addr : modified) {
            Account a = accounts.get(addr);
            if (a == null)
                a = Account.emptyAccount(addr, Uint256.ZERO);
            Trie<byte[], byte[]> s = contractStorageTrie.revert(a.getStorageRoot());

            Map<HexBytes, byte[]> map = storage.getOrDefault(addr, Collections.emptyMap());

            for (Map.Entry<HexBytes, byte[]> entry : map.entrySet()) {
                if (entry.getValue() == null || entry.getValue().length == 0) {
                    s.remove(entry.getKey().getBytes());
                } else {
                    s.put(entry.getKey().getBytes(), entry.getValue());
                }
            }

            a.setStorageRoot(s.commit());
            tmpTrie.put(addr, a);
        }

        for (Map.Entry<HexBytes, byte[]> entry : codeCache.entrySet()) {
            codeStore.put(entry.getKey().getBytes(), entry.getValue());
        }

        return tmpTrie.commit();
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
        return lookup(address).getBalance();
    }

    @Override
    public long getHeaderCreatedAt() {
        return headerCreatedAt;
    }

    @Override
    public void setBalance(HexBytes address, Uint256 balance) {
        if (modifiedAccounts.containsKey(address)) {
            modifiedAccounts.get(address).setBalance(balance);
            return;
        }
        Account a = lookup(address).clone();
        a.setBalance(balance);
        modifiedAccounts.put(address, a);
    }

    @Override
    public long getNonce(HexBytes address) {
        if (modifiedAccounts.containsKey(address)) {
            return modifiedAccounts.get(address).getNonce();
        }
        return lookup(address).getNonce();
    }

    @Override
    public void setNonce(HexBytes address, long nonce) {
        if (modifiedAccounts.containsKey(address)) {
            modifiedAccounts.get(address).setNonce(nonce);
            return;
        }
        Account a = lookup(address).clone();
        a.setNonce(nonce);
        modifiedAccounts.put(address, a);
    }

    @Override
    public List<ContractABI> getABI(HexBytes address) {
        if (modifiedAccounts.containsKey(address)) {
            return modifiedAccounts.get(address).getContractABIs();
        }
        return lookup(address).getContractABIs();
    }

    @Override
    public void setABI(HexBytes address, List<ContractABI> abi) {
        if (modifiedAccounts.containsKey(address)) {
            modifiedAccounts.get(address).setContractABIs(abi);
            return;
        }
        Account a = lookup(address).clone();
        a.setContractABIs(abi);
        modifiedAccounts.put(address, a);
    }


    @Override
    public long getInitialGas(int payloadSize) {
        return payloadSize / 1024;
    }

    @Override
    public Map<HexBytes, PreBuiltContract> getPreBuiltContracts() {
        return preBuiltContracts;
    }

    @Override
    public Map<HexBytes, Bios> getBios() {
        return bios;
    }

    @Override
    public void dbSet(HexBytes address, @NonNull byte[] key, @NonNull byte[] value) {
        if (key.length == 0)
            throw new RuntimeException("invalid key length = 0");
        modifiedStorage.putIfAbsent(address, new HashMap<>());
        modifiedStorage.get(address).put(HexBytes.fromBytes(key), HexBytes.fromBytes(value));
    }

    @Override
    public byte[] dbGet(HexBytes address, byte[] key) {
        if (key.length == 0)
            throw new RuntimeException("invalid key length = 0");

        // if has modified
        if (modifiedStorage.containsKey(address)) {
            // if it is delete mark
            HexBytes val = modifiedStorage.get(address).get(HexBytes.fromBytes(key));

            if (val != null) {
                // delete mark
                return val.getBytes();
            }

        }

        if (parentBackend != null) {
            return parentBackend.dbGet(address, key);
        }

        Account a = trie.get(address);

        if (a == null)
            return HexBytes.EMPTY_BYTES;

        byte[] v = contractStorageTrie.revert(a.getStorageRoot()).get(key);
        return v == null ? HexBytes.EMPTY_BYTES : v;
    }

    @Override
    public boolean dbHas(HexBytes address, @NonNull byte[] key) {
        if (key.length == 0)
            throw new RuntimeException("invalid key length = 0");
        byte[] val = dbGet(address, key);
        return val != null && val.length != 0;
    }

    @Override
    public HexBytes getContractCreatedBy(HexBytes address) {
        if (modifiedAccounts.containsKey(address)) {
            return modifiedAccounts.get(address).getCreatedBy();
        }
        return lookup(address).getCreatedBy();
    }

    @Override
    public void setContractCreatedBy(HexBytes address, HexBytes createdBy) {
        if (modifiedAccounts.containsKey(address)) {
            modifiedAccounts.get(address).setCreatedBy(createdBy);
            return;
        }
        Account a = lookup(address).clone();
        a.setCreatedBy(createdBy);
        modifiedAccounts.put(address, a);
    }

    @Override
    public void dbRemove(HexBytes address, @NonNull byte[] key) {
        if (key.length == 0)
            throw new RuntimeException("invalid key length = 0");
        dbSet(address, key, HexBytes.EMPTY_BYTES);
    }

    @Override
    public byte[] getCode(HexBytes address) {
        Account a = modifiedAccounts.get(address);
        if (a == null)
            a = lookup(address).clone();

        if (a.getContractHash() == null || a.getContractHash().length == 0)
            return HexBytes.EMPTY_BYTES;
        byte[] code = codeCache.get(HexBytes.fromBytes(a.getContractHash()));
        if (code != null && code.length != 0)
            return code;
        code = codeStore.get(a.getContractHash());
        return code == null ? HexBytes.EMPTY_BYTES : code;
    }

    @Override
    public void setCode(HexBytes address, byte[] code) {
        byte[] hash = CryptoContext.hash(code);
        if (modifiedAccounts.containsKey(address)) {
            modifiedAccounts.get(address).setContractHash(hash);
        } else {
            Account a = lookup(address).clone();
            a.setContractHash(hash);
            modifiedAccounts.put(address, a);
        }
        codeCache.put(HexBytes.fromBytes(hash), code);
    }

    @Override
    public void onEvent(HexBytes address, String eventName, RLPList eventData) {
        if (isStatic)
            return;
        events.putIfAbsent(address, new ArrayList<>());
        events.get(address).add(new AbstractMap.SimpleImmutableEntry<>(eventName, eventData));
    }

    @Override
    public Map<HexBytes, List<Map.Entry<String, RLPList>>> getEvents() {
        return events;
    }

    @Override
    public boolean isStatic() {
        return isStatic;
    }
}

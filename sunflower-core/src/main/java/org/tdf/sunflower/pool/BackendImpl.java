package org.tdf.sunflower.pool;

import lombok.*;
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

import java.util.*;

// backend for mining
@AllArgsConstructor
@NoArgsConstructor
public class BackendImpl implements Backend {
    private Header parent;
    @Getter
    private BackendImpl parentBackend;

    private Trie<HexBytes, Account> trie;
    private Trie<HexBytes, HexBytes> contractStorageTrie;

    private Map<HexBytes, Account> modifiedAccounts;
    private Map<HexBytes, Map<HexBytes, HexBytes>> modifiedStorage;
    private Map<HexBytes, PreBuiltContract> preBuiltContracts;
    private Map<HexBytes, Bios> bios;
    private boolean isStatic;

    private Store<HexBytes, HexBytes> codeStore;
    private Map<HexBytes, HexBytes> codeCache;
    private Map<HexBytes, List<Map.Entry<String, RLPList>>> events;

    @Setter
    @Getter
    private Long headerCreatedAt;

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

    private void mergeInternal(Map<HexBytes, Account> accounts, Map<HexBytes, Map<HexBytes, HexBytes>> storage) {
        if (parentBackend != null) {
            parentBackend.mergeInternal(accounts, storage);
        }

        modifiedAccounts.forEach(accounts::put);
        for (Map.Entry<HexBytes, Map<HexBytes, HexBytes>> entries : modifiedStorage.entrySet()) {
            storage.putIfAbsent(entries.getKey(), new HashMap<>());
            storage.get(entries.getKey()).putAll(entries.getValue());
        }
    }

    @Override
    public HexBytes merge() {
        Map<HexBytes, Account> accounts = new HashMap<>();
        Map<HexBytes, Map<HexBytes, HexBytes>> storage = new HashMap<>();
        mergeInternal(accounts, storage);

        Set<HexBytes> modified = new HashSet<>();
        modified.addAll(accounts.keySet());
        modified.addAll(storage.keySet());

        Trie<HexBytes, Account> tmpTrie = trie.revert(trie.getRootHash());

        for (HexBytes addr : modified) {
            Account a = accounts.get(addr);
            // some account has not touched, but storage modified
            if (a == null) {
                a = lookup(addr);
            }

            Trie<HexBytes, HexBytes> s = contractStorageTrie.revert(a.getStorageRoot());

            Map<HexBytes, HexBytes> map = storage.getOrDefault(addr, Collections.emptyMap());

            for (Map.Entry<HexBytes, HexBytes> entry : map.entrySet()) {
                if (entry.getValue() == null || entry.getValue().size() == 0) {
                    s.remove(entry.getKey());
                } else {
                    s.put(entry.getKey(), entry.getValue());
                }
            }

            a.setStorageRoot(s.commit());
            tmpTrie.put(addr, a);
        }

        for (Map.Entry<HexBytes, HexBytes> entry : codeCache.entrySet()) {
            codeStore.put(entry.getKey(), entry.getValue());
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
    public void dbSet(HexBytes address, @NonNull HexBytes key, @NonNull HexBytes value) {
        if (key.size() == 0)
            throw new RuntimeException("invalid key length = 0");
        modifiedStorage.putIfAbsent(address, new HashMap<>());
        modifiedStorage.get(address).put(key, value);
    }

    @Override
    public HexBytes dbGet(HexBytes address, HexBytes key) {
        if (key.size() == 0)
            throw new RuntimeException("invalid key length = 0");

        // if has modified
        if (modifiedStorage.containsKey(address)) {
            // if it is delete mark
            HexBytes val = modifiedStorage.get(address).get(key);

            if (val != null) {
                // delete mark
                return val;
            }

        }

        if (parentBackend != null) {
            return parentBackend.dbGet(address, key);
        }

        Account a = trie.get(address);

        if (a == null)
            return HexBytes.empty();

        HexBytes v = contractStorageTrie.revert(a.getStorageRoot()).get(key);
        return v == null ? HexBytes.empty() : v;
    }

    @Override
    public boolean dbHas(HexBytes address, @NonNull HexBytes key) {
        if (key.size() == 0)
            throw new RuntimeException("invalid key length = 0");
        HexBytes val = dbGet(address, key);
        return val != null && val.size() != 0;
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
    public void dbRemove(HexBytes address, @NonNull HexBytes key) {
        if (key.size() == 0)
            throw new RuntimeException("invalid key length = 0");
        dbSet(address, key, HexBytes.empty());
    }

    @Override
    public HexBytes getCode(HexBytes address) {
        Account a = modifiedAccounts.get(address);
        if (a == null)
            a = lookup(address).clone();

        if (a.getContractHash() == null || a.getContractHash().size() == 0)
            return HexBytes.empty();
        HexBytes code = codeCache.get(a.getContractHash());
        if (code != null && code.size() != 0)
            return code;
        code = codeStore.get(a.getContractHash());
        return code == null ? HexBytes.empty() : code;
    }

    @Override
    public void setCode(HexBytes address, HexBytes code) {
        byte[] hash = CryptoContext.hash(code.getBytes());
        if (modifiedAccounts.containsKey(address)) {
            modifiedAccounts.get(address).setContractHash(HexBytes.fromBytes(hash));
        } else {
            Account a = lookup(address).clone();
            a.setContractHash(HexBytes.fromBytes(hash));
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

package org.tdf.sunflower.state;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.tdf.common.serialize.Codecs;
import org.tdf.common.store.*;
import org.tdf.common.trie.SecureTrie;
import org.tdf.common.trie.Trie;
import org.tdf.common.types.Parameters;
import org.tdf.common.types.Uint256;
import org.tdf.common.util.ByteArrayMap;
import org.tdf.common.util.HexBytes;
import org.tdf.rlp.RLPCodec;
import org.tdf.rlp.RLPList;
import org.tdf.sunflower.Start;
import org.tdf.sunflower.types.CryptoContext;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.types.Transaction;
import org.tdf.sunflower.types.TransactionResult;
import org.tdf.sunflower.vm.abi.ContractCall;
import org.tdf.sunflower.vm.abi.ContractCallPayload;
import org.tdf.sunflower.vm.abi.ContractDeployPayload;
import org.tdf.sunflower.vm.hosts.Limit;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@Slf4j(topic = "trie")
@AllArgsConstructor
public class AccountTrie extends AbstractStateTrie<HexBytes, Account> implements ForkedStateTrie {
    private Trie<HexBytes, Account> trie;
    private Trie<byte[], byte[]> contractStorageTrie;
    private Store<byte[], byte[]> contractCodeStore;
    private List<PreBuiltContract> preBuiltContracts;
    private Map<HexBytes, Bios> biosList;
    private Map<HexBytes, Account> genesisStates;
    private Map<HexBytes, PreBuiltContract> preBuiltContractAddresses;
    private DatabaseStore db;
    private Store<byte[], byte[]> trieStore;
    private HexBytes genesisRoot;

    // memory cached
    private CachedStore<byte[], byte[]> trieCache;
    private NoDeleteCachedStore<byte[], byte[]> contractStorageCache;
    private CachedStore<byte[], byte[]> contractCodeCache;
    private Trie<HexBytes, Account> dirtyTrie;
    private byte[] currentRoot;

    @SneakyThrows
    public AccountTrie(
            DatabaseStore db,
            Store<byte[], byte[]> contractCodeStore,
            Trie<byte[], byte[]> contractStorageTrie,
            Map<HexBytes, Account> genesisStates,
            List<PreBuiltContract> preBuiltContracts,
            List<Bios> biosList,
            boolean secure
    ) {
        this.db = db;
        this.trieStore = new NoDeleteBatchStore<>(db);

        this.trie = Trie.<HexBytes, Account>builder()
                .hashFunction(CryptoContext::hash)
                .store(trieStore)
                .keyCodec(Codecs.newRLPCodec(HexBytes.class))
                .valueCodec(Codecs.newRLPCodec(Account.class))
                .build();

        if (secure)
            trie = new SecureTrie<>(trie, CryptoContext::hash);

        this.contractStorageTrie = contractStorageTrie;
        this.contractCodeStore = contractCodeStore;
        this.preBuiltContracts = preBuiltContracts;
        this.biosList = biosList.stream().collect(
                Collectors.toMap(x -> x.getGenesisAccount().getAddress(), Function.identity())
        );
        this.genesisStates = new HashMap<>(genesisStates);

        List<CommonUpdater> commonUpdaters = new ArrayList<>();
        commonUpdaters.addAll(preBuiltContracts);
        commonUpdaters.addAll(biosList);
        this.preBuiltContractAddresses = new HashMap<>();

        for (CommonUpdater updater : commonUpdaters) {
            Account genesisAccount = updater.getGenesisAccount();
            Trie<byte[], byte[]> trie = contractStorageTrie.revert();
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

        // sync to genesis
        Trie<HexBytes, Account> tmp = this.trie.revert();
        this.genesisStates.forEach(tmp::put);
        this.genesisRoot = HexBytes.fromBytes(tmp.commit());
        log.info("genesis states = {}", Start.MAPPER.writeValueAsString(genesisStates));
        log.info("genesis state root = " + genesisRoot);
        tmp.flush();
    }

    private Trie<byte[], byte[]> getDirtyContractStorageTrie(byte[] storageRoot) {
        return contractStorageTrie.revert(
                storageRoot,
                this.contractStorageCache
        );
    }

    @Override
    DatabaseStore getDB() {
        return this.db;
    }

    @Override
    protected Trie<HexBytes, Account> commitInternal(byte[] parentRoot, Map<HexBytes, Account> data) {
        return super.commitInternal(parentRoot, data);
    }

    private void clearCache() {
        Map<byte[], byte[]> trieCache = new ByteArrayMap<>();
        Map<byte[], byte[]> contractStorageCacheMap = new ByteArrayMap<>();
        Map<byte[], byte[]> contractCodeCache = new ByteArrayMap<>();
        this.trieCache = new CachedStore<>(trie.getStore(), () -> trieCache);;
        this.contractStorageCache = new NoDeleteCachedStore<>(this.contractStorageTrie.getStore(), () -> contractStorageCacheMap);;
        this.contractCodeCache = new CachedStore<>(contractCodeStore, () -> contractCodeCache);
        this.dirtyTrie = trie.revert(this.currentRoot, this.trieCache);
    }

    @Override
    public ForkedStateTrie fork(byte[] parentRoot) {
        AccountTrie ret = new AccountTrie(
                trie,
                contractStorageTrie,
                this.contractCodeStore,
                preBuiltContracts,
                biosList,
                genesisStates,
                preBuiltContractAddresses,
                db,
                this.trieStore,
                genesisRoot,
                null,
                null,
                null,
                null,
                parentRoot
        );
        ret.clearCache();
        return ret;
    }

    public void increaseNonce(Transaction tx) {
        Map<HexBytes, Account> states = this.dirtyTrie.asMap();
        Account state = states.get(tx.getFromAddress());
        if (state.getNonce() + 1 != tx.getNonce())
            throw new RuntimeException("the nonce of transaction should be " + (state.getNonce() + 1)
                    + " while " + tx.getNonce() + " received");
        state.setNonce(tx.getNonce());
        states.put(state.getAddress(), state);
    }

    public void updateTransfer(HexBytes fromAddr, HexBytes toAddr, Uint256 amount, Uint256 fee) {
        Map<HexBytes, Account> states = this.dirtyTrie.asMap();

        if (amount.compareTo(Uint256.ZERO) == 0)
            throw new RuntimeException("transfer amount = 0");

        Account from = states.get(fromAddr);
        from.subBalance(amount);
        from.subBalance(fee);

        Account to = states.get(toAddr);
        if (to.getCreatedBy() != null && !to.getCreatedBy().isEmpty())
            throw new RuntimeException("transfer to contract address is not allowed");

        to.addBalance(amount);
        states.put(fromAddr, from);
        states.put(toAddr, to);
    }

    public TransactionResult update(Header header, Transaction tx) {

        try {
            TransactionResult r = updateInternal(header, tx);
            this.currentRoot = dirtyTrie.commit();
            flush();
            return r;
        } catch (Exception e) {
            throw e;
        }finally {
            clearCache();
        }
    }

    private TransactionResult updateInternal(Header header, Transaction tx) {
        Map<HexBytes, Account> states = this.dirtyTrie.asMap();
        if (tx.getType() == Transaction.Type.COIN_BASE.code && header.getHeight() != tx.getNonce()) {
            throw new RuntimeException("nonce of coinbase transaction should be " + header.getHeight());
        }
        switch (Transaction.Type.TYPE_MAP.get(tx.getType())) {
            case TRANSFER: {
                states.putIfAbsent(tx.getTo(), Account.emptyAccount(tx.getTo()));
                increaseNonce(tx);
                updateTransfer(
                        tx.getFromAddress(),
                        tx.getTo(),
                        tx.getAmount(),
                        tx.getFee()
                );
                return new TransactionResult(Transaction.TRANSFER_GAS, RLPList.createEmpty(), Collections.emptyList(), tx.getFee());
            }
            case COIN_BASE: {
                states.putIfAbsent(tx.getTo(), Account.emptyAccount(tx.getTo()));
                updateCoinBase(header, tx);
                return TransactionResult.EMPTY;
            }
            case CONTRACT_DEPLOY:
                return updateDeploy(states, header, tx);
            case CONTRACT_CALL:
                states.putIfAbsent(tx.getFromAddress(), Account.emptyAccount(tx.getFromAddress()));
                increaseNonce(tx);
                return updateContractCall(header, tx);
        }
        throw new RuntimeException("unknown type " + tx.getType());
    }


    private void updateCoinBase(Header header, Transaction t) {
        Map<HexBytes, Account> accounts = this.dirtyTrie.asMap();

        Account to = accounts.get(t.getTo());
        to.addBalance(t.getAmount());
        accounts.put(to.getAddress(), to);

        for (Bios bios : biosList.values()) {
            Account a = accounts.get(bios.getGenesisAccount().getAddress());
            Trie<byte[], byte[]> before = getDirtyContractStorageTrie(a.getStorageRoot());
            bios.update(header, before);
            byte[] root = before.commit();
            before.flush();
            a.setStorageRoot(root);
            accounts.put(a.getAddress(), a);
        }
    }

    private TransactionResult updateDeploy(Map<HexBytes, Account> accounts, Header header, Transaction t) {
        ContractDeployPayload contractDeployPayload =
                RLPCodec.decode(t.getPayload().getBytes(), ContractDeployPayload.class);

        Limit limit = new Limit(0, 0, t.getGasLimit(), t.getPayload().size() / 1024);

        // execute constructor of contract
        ContractCall contractCall = new ContractCall(
                accounts, header,
                t, this::getDirtyContractStorageTrie, this.contractCodeCache,
                limit, 0, t.getFromAddress(),
                false
        );

        TransactionResult ret = contractCall.call(
                HexBytes.fromBytes(contractDeployPayload.getBinary()),
                "init",
                contractDeployPayload.getParameters(),
                t.getAmount(),
                false,
                contractDeployPayload.getContractABIs()
        );


        // restore from map
        Account createdBy = accounts.get(t.getFromAddress());
        Account contractAccount = accounts.get(t.createContractAddress());

        // estimate gas
        Uint256 fee = Uint256.of(limit.getGas()).safeMul(t.getGasPrice());
        createdBy.subBalance(fee);
        ret.setFee(fee);
        accounts.put(createdBy.getAddress(), createdBy);
        log.info("deploy contract at " + contractAccount.getAddress() + " success");
        return ret;
    }


    private TransactionResult updateContractCall(Header header, Transaction t) {

        Map<HexBytes, Account> accounts = getDirtyTrie().asMap();
        Account originAccount = accounts.get(t.getFromAddress());
        Account contractAccount = accounts.get(t.getTo());
        if (contractAccount == null) {
            throw new RuntimeException("contract " + t.getTo() + " not found");
        }

        if (preBuiltContractAddresses.containsKey(contractAccount.getAddress())) {
            Uint256 fee = Uint256.of(Transaction.BUILTIN_CALL_GAS).safeMul(t.getGasPrice());
            Trie<byte[], byte[]> before =
                    getDirtyContractStorageTrie(contractAccount.getStorageRoot());

            contractAccount.addBalance(t.getAmount());
            originAccount.subBalance(t.getAmount());
            originAccount.subBalance(fee);
            accounts.put(contractAccount.getAddress(), contractAccount);
            accounts.put(originAccount.getAddress(), originAccount);

            PreBuiltContract updater = preBuiltContractAddresses.get(contractAccount.getAddress());
            updater.update(header, t, accounts, before);
            byte[] root = before.commit();
            contractAccount.setStorageRoot(root);
            accounts.put(contractAccount.getAddress(), contractAccount);
            return new TransactionResult(Transaction.BUILTIN_CALL_GAS, RLPList.createEmpty(), Collections.emptyList(), fee);
        }

        ContractCallPayload callPayload = RLPCodec.decode(t.getPayload().getBytes(), ContractCallPayload.class);

        // execute method
        Limit limit = new Limit(0, 0, t.getGasLimit(), t.getPayload().size() / 1024);
        ContractCall contractCall = new ContractCall(
                accounts, header,
                t, this::getDirtyContractStorageTrie,
                this.contractCodeCache,
                limit, 0, t.getFromAddress(),
                false
        );

        TransactionResult result = contractCall.call(
                contractAccount.getAddress(),
                callPayload.getMethod(),
                callPayload.getParameters(),
                t.getAmount(),
                false,
                null
        );

        contractAccount = accounts.get(t.getTo());
        Account caller = accounts.get(t.getFromAddress());

        Uint256 fee = Uint256.of(limit.getGas()).safeMul(t.getGasPrice());
        caller.subBalance(fee);
        accounts.put(contractAccount.getAddress(), contractAccount);
        accounts.put(caller.getAddress(), caller);
        result.setFee(fee);
        return result;
    }

    private void flush() {
        this.trieCache.flush();
        this.contractStorageCache.flush();
        this.contractCodeCache.flush();
    }

    public RLPList call(HexBytes address, String method, Parameters parameters) {
        // execute method
        Limit limit = new Limit();
        ContractCall contractCall = new ContractCall(
                this.dirtyTrie.asMap(), null,
                null, this::getDirtyContractStorageTrie,
                this.contractCodeCache,
                limit, 0, null,
                true
        );

        return contractCall.call(
                address,
                method,
                parameters, Uint256.ZERO,
                false, null
        ).getReturns();
    }
}

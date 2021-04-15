package org.tdf.sunflower.state;

import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;
import org.tdf.common.serialize.Codec;
import org.tdf.common.serialize.Codecs;
import org.tdf.common.store.PrefixStore;
import org.tdf.common.store.Store;
import org.tdf.common.trie.Trie;
import org.tdf.common.util.ByteArrayMap;
import org.tdf.common.util.HexBytes;
import org.tdf.rlp.RLPCodec;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.types.Transaction;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * used for node join/exit
 */
public class Authentication implements PreBuiltContract {
    static final byte[] NODES_KEY = "nodes".getBytes(StandardCharsets.US_ASCII);
    static final byte[] PENDING_NODES_KEY = "pending".getBytes(StandardCharsets.US_ASCII);
    private final Collection<? extends HexBytes> nodes;
    private final HexBytes contractAddress;
    @Setter
    private StateTrie<HexBytes, Account> accountTrie;
    @Setter
    private Trie<byte[], byte[]> contractStorageTrie;

    public Authentication(
            @NonNull Collection<? extends HexBytes> nodes,
            @NonNull HexBytes contractAddress
    ) {
        this.nodes = nodes;
        this.contractAddress = contractAddress;
    }

    static int divideAndCeil(int a, int b) {
        int ret = a / b;
        if (a % b != 0)
            return ret + 1;
        return ret;
    }

    private byte[] getValue(byte[] stateRoot, byte[] key) {
        Account a = accountTrie.get(stateRoot, this.contractAddress).get();
        Store<byte[], byte[]> db = contractStorageTrie.revert(a.getStorageRoot());
        return db.get(key).get();
    }

    public List<HexBytes> getNodes(byte[] stateRoot) {
        byte[] v = getValue(stateRoot, NODES_KEY);
        return Arrays.asList(RLPCodec.decode(v, HexBytes[].class));
    }

    public Map<HexBytes, TreeSet<HexBytes>> getPending(byte[] stateRoot) {
        Account a = accountTrie.get(stateRoot, contractAddress).get();
        Store<byte[], byte[]> contractStorage = contractStorageTrie.revert(a.getStorageRoot());
        return new HashMap<>(getPendingStore(contractStorage).asMap());
    }

    @SneakyThrows
    public Store<HexBytes, TreeSet<HexBytes>> getPendingStore(Store<byte[], byte[]> contractStorage) {
        return new PrefixStore<>(
                contractStorage,
                PENDING_NODES_KEY,
                Codecs.newRLPCodec(HexBytes.class),
                Codec.newInstance(
                        RLPCodec::encode,
                        x -> new TreeSet<>(Arrays.asList(RLPCodec.decode(x, HexBytes[].class)))
                )
        );
    }

    @Override
    public Account getGenesisAccount() {
        return Account.emptyContract(this.contractAddress);
    }

    @Override
    @SneakyThrows
    public void update(Header header, Transaction transaction, Map<HexBytes, Account> accounts, Store<byte[], byte[]> contractStorage) {
        Method m = Method.values()[transaction.getPayload().get(0)];
        List<HexBytes> nodes = new ArrayList<>(Arrays.asList(RLPCodec.decode(contractStorage.get(NODES_KEY).get(), HexBytes[].class)));
        Store<HexBytes, TreeSet<HexBytes>> pending = getPendingStore(contractStorage);

        switch (m) {
            case JOIN_NODE: {
                HexBytes fromAddr = transaction.getFromAddress();
                if (nodes.contains(fromAddr))
                    throw new RuntimeException("authentication contract error: " + fromAddr + " has already in nodes");

                if (pending.containsKey(fromAddr))
                    throw new RuntimeException("authentication contract error: " + fromAddr + " has already in pending");

                pending.put(fromAddr, new TreeSet<>());
                break;
            }
            case APPROVE_JOIN: {
                if (!nodes.contains(transaction.getFromAddress())) {
                    throw new RuntimeException("authentication contract error: cannot approve " + transaction.getFromAddress() + " is not in nodes list");
                }
                HexBytes toApprove = transaction.getPayload().slice(1);

                Optional<TreeSet<HexBytes>> approves = pending.get(toApprove);
                if (!approves.isPresent())
                    throw new RuntimeException("authentication contract error: cannot approve " + toApprove + " not in pending");

                if (approves.get().contains(transaction.getFromAddress())) {
                    throw new RuntimeException("authentication contract error: cannot approve " + toApprove + " has approved");
                }

                approves.get().add(transaction.getFromAddress());
                if (transaction.getTo().equals(Constants.VALIDATOR_CONTRACT_ADDR)){
                    pending.remove(toApprove);
                    nodes.add(toApprove);
                }else {
                    if (approves.get().size() >= divideAndCeil(nodes.size() * 2, 3)) {
                        pending.remove(toApprove);
                        nodes.add(toApprove);
                    } else {
                        pending.put(toApprove, approves.get());
                    }
                }
                contractStorage.put(NODES_KEY, RLPCodec.encode(nodes));
                break;
            }
            case EXIT: {
                HexBytes fromAddr = transaction.getFromAddress();
                if (!nodes.contains(fromAddr))
                    throw new RuntimeException("authentication contract error: " + fromAddr + " not in nodes");
                if (nodes.size() <= 1)
                    throw new RuntimeException("authentication contract error: cannot exit, at least one miner");

                nodes.remove(fromAddr);
                contractStorage.put(NODES_KEY, RLPCodec.encode(nodes));
                break;
            }
        }
    }

    @Override
    public Map<byte[], byte[]> getGenesisStorage() {
        Map<byte[], byte[]> ret = new ByteArrayMap<>();
        ret.put(NODES_KEY, RLPCodec.encode(this.nodes));
        return ret;
    }

    public enum Method {
        JOIN_NODE,
        APPROVE_JOIN,
        EXIT
    }
}

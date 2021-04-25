package org.tdf.sunflower.state;

import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;
import org.tdf.common.serialize.Codec;
import org.tdf.common.serialize.Codecs;
import org.tdf.common.store.PrefixStore;
import org.tdf.common.store.Store;
import org.tdf.common.trie.Trie;
import org.tdf.common.types.Uint256;
import org.tdf.common.util.ByteArrayMap;
import org.tdf.common.util.HexBytes;
import org.tdf.rlp.RLPCodec;
import org.tdf.sunflower.vm.Backend;
import org.tdf.sunflower.vm.CallData;

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
        Account a = accountTrie.get(stateRoot, this.contractAddress);
        Store<byte[], byte[]> db = contractStorageTrie.revert(a.getStorageRoot());
        return db.get(key);
    }

    public List<HexBytes> getNodes(byte[] stateRoot) {
        byte[] v = getValue(stateRoot, NODES_KEY);
        return Arrays.asList(RLPCodec.decode(v, HexBytes[].class));
    }

    public Map<HexBytes, TreeSet<HexBytes>> getPending(byte[] stateRoot) {
        Account a = accountTrie.get(stateRoot, contractAddress);
        Store<byte[], byte[]> contractStorage = contractStorageTrie.revert(a.getStorageRoot());
        Map<HexBytes, TreeSet<HexBytes>> ret = new HashMap<>();
        for (Map.Entry<HexBytes, TreeSet<HexBytes>> entry : getPendingStore(contractStorage)) {
            ret.put(entry.getKey(), entry.getValue());
        }
        return ret;
    }

    @SneakyThrows
    public PrefixStore<HexBytes, TreeSet<HexBytes>> getPendingStore(Store<byte[], byte[]> contractStorage) {
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


    private byte[] getByNodesKey(Backend backend) {
        return backend.dbGet(contractAddress, NODES_KEY);
    }


    @Override
    public Account getGenesisAccount() {
        return Account.emptyAccount(this.contractAddress, Uint256.ZERO);
    }

    @Override
    @SneakyThrows
    public void update(Backend backend, CallData callData) {
        HexBytes payload = callData.getPayload();

        Method m = Method.values()[callData.getPayload().get(0)];
        List<HexBytes> nodes = new ArrayList<>(Arrays.asList(RLPCodec.decode(getByNodesKey(backend), HexBytes[].class)));
        Store<HexBytes, TreeSet<HexBytes>> pending = getPendingStore(backend.getAsStore(contractAddress));

        switch (m) {
            case JOIN_NODE: {
                HexBytes fromAddr = callData.getCaller();
                if (nodes.contains(fromAddr))
                    throw new RuntimeException("authentication contract error: " + fromAddr + " has already in nodes");

                if (pending.get(fromAddr) == null)
                    throw new RuntimeException("authentication contract error: " + fromAddr + " has already in pending");

                pending.put(fromAddr, new TreeSet<>());
                break;
            }
            case APPROVE_JOIN: {
                HexBytes toApprove = payload.slice(1);

                if (callData.getTo().equals(Constants.VALIDATOR_CONTRACT_ADDR)) {
                    pending.remove(toApprove);
                    nodes.add(toApprove);
                    backend.dbSet(contractAddress, NODES_KEY, RLPCodec.encode(nodes));
                    break;
                }

                if (!nodes.contains(callData.getCaller())) {
                    throw new RuntimeException("authentication contract error: cannot approve " + callData.getCaller() + " is not in nodes list");
                }

                TreeSet<HexBytes> approves = pending.get(toApprove);
                if (approves == null)
                    throw new RuntimeException("authentication contract error: cannot approve " + toApprove + " not in pending");

                if (approves.contains(callData.getCaller())) {
                    throw new RuntimeException("authentication contract error: cannot approve " + toApprove + " has approved");
                }

                approves.add(callData.getCaller());

                if (approves.size() >= divideAndCeil(nodes.size() * 2, 3)) {
                    pending.remove(toApprove);
                    nodes.add(toApprove);
                } else {
                    pending.put(toApprove, approves);
                }

                backend.dbSet(contractAddress, NODES_KEY, RLPCodec.encode(nodes));
                break;
            }
            case EXIT: {

                HexBytes fromAddr = callData.getCaller();
                if (!nodes.contains(fromAddr))
                    throw new RuntimeException("authentication contract error: " + fromAddr + " not in nodes");
                if (nodes.size() <= 1)
                    throw new RuntimeException("authentication contract error: cannot exit, at least one miner");

                nodes.remove(fromAddr);
                backend.dbSet(contractAddress, NODES_KEY, RLPCodec.encode(nodes));
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

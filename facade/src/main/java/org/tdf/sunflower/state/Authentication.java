package org.tdf.sunflower.state;

import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;
import org.tdf.common.store.Store;
import org.tdf.common.trie.Trie;
import org.tdf.common.util.ByteArrayMap;
import org.tdf.common.util.HexBytes;
import org.tdf.rlp.Container;
import org.tdf.rlp.RLPCodec;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.types.Transaction;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * used for node join/exit
 */
public class Authentication implements PreBuiltContract {
    public enum Method {
        JOIN_NODE,
        APPROVE_JOIN,
        EXIT
    }

    private final Collection<? extends HexBytes> nodes;

    private final HexBytes contractAddress;

    @Setter
    private StateTrie<HexBytes, Account> accountTrie;

    @Setter
    private Trie<byte[], byte[]> contractStorageTrie;


    public Authentication(@NonNull Collection<? extends HexBytes> nodes, @NonNull HexBytes contractAddress) {
        this.nodes = nodes;
        this.contractAddress = contractAddress;
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


    public static class Dummy {
        public TreeMap<HexBytes, TreeSet<HexBytes>> dummy;
    }

    @SneakyThrows
    public TreeMap<HexBytes, TreeSet<HexBytes>> getPending(byte[] stateRoot) {
        byte[] v = getValue(stateRoot, PENDING_NODES_KEY);
        return (TreeMap<HexBytes, TreeSet<HexBytes>>) RLPCodec.decodeContainer(v, Container.fromField(Dummy.class.getField("dummy")));
    }

    static final byte[] NODES_KEY = "nodes".getBytes(StandardCharsets.US_ASCII);

    static final byte[] PENDING_NODES_KEY = "pending".getBytes(StandardCharsets.US_ASCII);


    @Override
    public Account getGenesisAccount() {
        return Account.emptyContract(this.contractAddress);
    }

    @Override
    @SneakyThrows
    public void update(Header header, Transaction transaction, Map<HexBytes, Account> accounts, Store<byte[], byte[]> contractStorage) {
        Method m = Method.values()[transaction.getPayload().get(0)];
        List<HexBytes> nodes = new ArrayList<>(Arrays.asList(RLPCodec.decode(contractStorage.get(NODES_KEY).get(), HexBytes[].class)));
        TreeMap<HexBytes, TreeSet<HexBytes>> pending =
                (TreeMap<HexBytes, TreeSet<HexBytes>>)
                        RLPCodec.decodeContainer(
                                contractStorage.get(PENDING_NODES_KEY).get(),
                                Container.fromField(Dummy.class.getField("dummy"))
                        );

        switch (m) {
            case JOIN_NODE: {
                HexBytes fromAddr = transaction.getFromAddress();
                if (nodes.contains(fromAddr))
                    throw new RuntimeException(fromAddr + " has already in nodes");

                if (pending.containsKey(fromAddr))
                    throw new RuntimeException(fromAddr + " has already in pending");

                pending.put(fromAddr, new TreeSet<>());
                contractStorage.put(PENDING_NODES_KEY, RLPCodec.encode(pending));
                break;
            }
            case APPROVE_JOIN: {
                if (!nodes.contains(transaction.getFromAddress())) {
                    throw new RuntimeException("cannot approve " + transaction.getFromAddress() + " is not in nodes list");
                }
                HexBytes approved = transaction.getPayload().slice(1);
                if (!pending.containsKey(approved))
                    throw new RuntimeException("cannot approve " + approved + " not in pending");

                if (pending.get(approved).contains(transaction.getFromAddress())) {
                    throw new RuntimeException("cannot approve " + approved + " has approved");
                }

                pending.get(approved).add(transaction.getFromAddress());
                if (pending.get(approved).size() >= divideAndCeil(nodes.size() * 2, 3)) {
                    pending.remove(approved);
                    nodes.add(approved);
                }
                contractStorage.put(PENDING_NODES_KEY, RLPCodec.encode(pending));
                contractStorage.put(NODES_KEY, RLPCodec.encode(nodes));
                break;
            }
            case EXIT:{
                HexBytes fromAddr = transaction.getFromAddress();
                if (!nodes.contains(fromAddr))
                    throw new RuntimeException(fromAddr + " not in nodes");
                if(nodes.size() <= 1)
                    throw new RuntimeException("cannot exit, at least one miner");

                nodes.remove(fromAddr);
                contractStorage.put(NODES_KEY, RLPCodec.encode(nodes));
                break;
            }
        }
    }

    static int divideAndCeil(int a, int b) {
        int ret = a / b;
        if (a % b != 0)
            return ret + 1;
        return ret;
    }

    @Override
    public Map<byte[], byte[]> getGenesisStorage() {
        Map<byte[], byte[]> ret = new ByteArrayMap<>();
        ret.put(NODES_KEY, RLPCodec.encode(new TreeSet<>(this.nodes)));
        ret.put(PENDING_NODES_KEY, RLPCodec.encode(new HashMap<>()));
        return ret;
    }
}
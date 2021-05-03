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
import org.tdf.common.util.FastByteComparisons;
import org.tdf.common.util.HexBytes;
import org.tdf.common.util.RLPUtil;
import org.tdf.rlp.RLPCodec;
import org.tdf.sunflower.vm.Backend;
import org.tdf.sunflower.vm.CallData;
import org.tdf.sunflower.vm.abi.Abi;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * used for node join/exit
 */
public class Authentication implements PreBuiltContract {
    public static final String ABI_JSON = "[{\"inputs\":[{\"internalType\":\"address\",\"name\":\"dst\",\"type\":\"address\"}],\"name\":\"approve\",\"outputs\":[],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[],\"name\":\"approved\",\"outputs\":[{\"internalType\":\"address[]\",\"name\":\"\",\"type\":\"address[]\"}],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[],\"name\":\"exit\",\"outputs\":[],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[],\"name\":\"join\",\"outputs\":[],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"address\",\"name\":\"dst\",\"type\":\"address\"}],\"name\":\"pending\",\"outputs\":[{\"internalType\":\"address[]\",\"name\":\"\",\"type\":\"address[]\"}],\"stateMutability\":\"view\",\"type\":\"function\"}]";
    public static final Abi ABI = Abi.fromJson(ABI_JSON);


    static final HexBytes NODES_KEY = HexBytes.fromBytes("nodes".getBytes(StandardCharsets.US_ASCII));
    static final HexBytes PENDING_NODES_KEY = HexBytes.fromBytes("pending".getBytes(StandardCharsets.US_ASCII));
    private final Collection<? extends HexBytes> nodes;
    private final HexBytes contractAddress;
    @Setter
    private StateTrie<HexBytes, Account> accountTrie;
    @Setter
    private Trie<HexBytes, HexBytes> contractStorageTrie;

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

    private HexBytes getValue(HexBytes stateRoot, HexBytes key) {
        Account a = accountTrie.get(stateRoot, this.contractAddress);
        Store<HexBytes, HexBytes> db = contractStorageTrie.revert(a.getStorageRoot());
        return db.get(key);
    }

    public List<HexBytes> getNodes(HexBytes stateRoot) {
        HexBytes v = getValue(stateRoot, NODES_KEY);
        return Arrays.asList(
                RLPUtil.decode(v, HexBytes[].class)
        );
    }

    public Map<HexBytes, TreeSet<HexBytes>> getPending(HexBytes stateRoot) {
        Account a = accountTrie.get(stateRoot, contractAddress);
        Store<HexBytes, HexBytes> contractStorage = contractStorageTrie.revert(a.getStorageRoot());
        Map<HexBytes, TreeSet<HexBytes>> ret = new HashMap<>();
        for (Map.Entry<HexBytes, TreeSet<HexBytes>> entry : getPendingStore(contractStorage)) {
            ret.put(entry.getKey(), entry.getValue());
        }
        return ret;
    }

    @SneakyThrows
    public PrefixStore<HexBytes, TreeSet<HexBytes>> getPendingStore(Store<HexBytes, HexBytes> contractStorage) {
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


    private HexBytes getByNodesKey(Backend backend) {
        return backend.dbGet(contractAddress, NODES_KEY);
    }


    @Override
    public Account getGenesisAccount() {
        return Account.emptyAccount(this.contractAddress, Uint256.ZERO);
    }

    @Override
    @SneakyThrows
    public byte[] call(Backend backend, CallData callData) {
        byte[] selector = callData.getSelector();
        Abi.Function func = Objects.requireNonNull(
                ABI.findFunction(x -> FastByteComparisons.equal(x.encodeSignature(), selector))
        );

        List<HexBytes> nodes = new ArrayList<>(
                Arrays.asList(RLPCodec.decode(getByNodesKey(backend).getBytes(), HexBytes[].class))
        );
        Store<HexBytes, TreeSet<HexBytes>> pending = getPendingStore(backend.getAsStore(contractAddress));

        switch (func.name) {
            case "approved": {
                return Abi.Entry.Param.encodeList(
                        func.outputs,
                        nodes.stream().map(HexBytes::getBytes).collect(Collectors.toList())
                );
            }
            case "pending": {
                byte[] dstBytes = (byte[]) func.decode(callData.getData().getBytes()).get(0);
                HexBytes dst = HexBytes.fromBytes(dstBytes);
                Set<HexBytes> r = pending.get(dst);
                if(r == null)
                    r = Collections.emptySet();
                return Abi.Entry.Param.encodeList(
                        func.outputs,
                        r.stream().map(HexBytes::getBytes).collect(Collectors.toList())
                );
            }
            case "join": {
                HexBytes fromAddr = callData.getCaller();
                if (nodes.contains(fromAddr))
                    throw new RuntimeException("authentication contract error: " + fromAddr + " has already in nodes");

                if (pending.get(fromAddr) == null)
                    throw new RuntimeException("authentication contract error: " + fromAddr + " has already in pending");

                pending.put(fromAddr, new TreeSet<>());
                return HexBytes.EMPTY_BYTES;
            }
            case "approve": {
                byte[] toApproveBytes = (byte[]) (func.decode(callData.getData().getBytes()).get(0));
                HexBytes toApprove = HexBytes.fromBytes(toApproveBytes);

                if (callData.getTo().equals(Constants.VALIDATOR_CONTRACT_ADDR)) {
                    pending.remove(toApprove);
                    nodes.add(toApprove);
                    backend.dbSet(contractAddress, NODES_KEY, HexBytes.fromBytes(RLPCodec.encode(nodes)));
                    return HexBytes.EMPTY_BYTES;
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

                backend.dbSet(contractAddress, NODES_KEY, HexBytes.fromBytes(RLPCodec.encode(nodes)));
                return HexBytes.EMPTY_BYTES;
            }
            case "exit": {

                HexBytes fromAddr = callData.getCaller();
                if (!nodes.contains(fromAddr))
                    throw new RuntimeException("authentication contract error: " + fromAddr + " not in nodes");
                if (nodes.size() <= 1)
                    throw new RuntimeException("authentication contract error: cannot exit, at least one miner");

                nodes.remove(fromAddr);
                backend.dbSet(contractAddress, NODES_KEY, HexBytes.fromBytes(RLPCodec.encode(nodes)));
                return HexBytes.EMPTY_BYTES;
            }
            default:
                throw new RuntimeException("method not found");
        }
    }

    @Override
    public Map<HexBytes, HexBytes> getGenesisStorage() {
        Map<HexBytes, HexBytes> ret = new HashMap();
        ret.put(NODES_KEY, HexBytes.fromBytes(RLPCodec.encode(this.nodes)));
        return ret;
    }

    public enum Method {
        JOIN_NODE,
        APPROVE_JOIN,
        EXIT
    }

}

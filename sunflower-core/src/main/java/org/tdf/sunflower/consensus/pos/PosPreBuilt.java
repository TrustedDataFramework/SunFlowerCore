package org.tdf.sunflower.consensus.pos;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.tdf.common.serialize.Codec;
import org.tdf.common.serialize.Codecs;
import org.tdf.common.store.PrefixStore;
import org.tdf.common.store.Store;
import org.tdf.common.types.Uint256;
import org.tdf.common.util.ByteArrayMap;
import org.tdf.common.util.ByteUtil;
import org.tdf.common.util.HexBytes;
import org.tdf.common.util.RLPUtil;
import org.tdf.rlp.RLPCodec;
import org.tdf.rlp.RLPList;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.AccountTrie;
import org.tdf.sunflower.state.Constants;
import org.tdf.sunflower.state.PreBuiltContract;
import org.tdf.sunflower.vm.Backend;
import org.tdf.sunflower.vm.CallData;
import org.tdf.sunflower.vm.abi.Abi;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j(topic = "pos")
public class PosPreBuilt implements PreBuiltContract {
    public static final HexBytes NODE_INFO_KEY = HexBytes.fromBytes("nodes".getBytes(StandardCharsets.US_ASCII));
    public static final HexBytes VOTE_INFO_KEY = HexBytes.fromBytes("votes".getBytes(StandardCharsets.US_ASCII));

    private final Map<HexBytes, NodeInfo> nodes;
    @Setter
    private AccountTrie accountTrie;

    public PosPreBuilt(@NonNull Map<HexBytes, NodeInfo> nodes) {
        this.nodes = nodes;
    }

    private static <T> Map.Entry<Integer, T> findFirst(List<? extends T> c, Predicate<T> predicate) {
        for (int i = 0; i < c.size(); i++) {
            if (predicate.test(c.get(i))) {
                return new AbstractMap.SimpleImmutableEntry<>(i, c.get(i));
            }
        }
        return new AbstractMap.SimpleImmutableEntry<>(-1, null);
    }

    private HexBytes getValue(HexBytes stateRoot, HexBytes key) {
        Account a = accountTrie.get(stateRoot, Constants.POS_CONTRACT_ADDR);
        Store<HexBytes, HexBytes> db = accountTrie.getContractStorageTrie().revert(a.getStorageRoot());
        return db.get(key);
    }

    public List<HexBytes> getNodes(HexBytes stateRoot) {
        HexBytes v = getValue(stateRoot, NODE_INFO_KEY);
        return Arrays.stream(RLPUtil.decode(v, NodeInfo[].class)).map(NodeInfo::getAddress).collect(Collectors.toList());
    }

    public List<NodeInfo> getNodeInfos(HexBytes stateRoot) {
        HexBytes v = getValue(stateRoot, NODE_INFO_KEY);
        return Arrays.asList(RLPUtil.decode(
                v, NodeInfo[].class));
    }

    public Optional<VoteInfo> getVoteInfo(HexBytes stateRoot, HexBytes txHash) {
        Account a = accountTrie.get(stateRoot, Constants.POS_CONTRACT_ADDR);
        Store<HexBytes, HexBytes> db = accountTrie.getContractStorageTrie().revert(a.getStorageRoot());
        PrefixStore<HexBytes, VoteInfo> store = getVoteInfoStore(db);
        return Optional.ofNullable(store.get(txHash));
    }

    public PrefixStore<HexBytes, VoteInfo> getVoteInfoStore(Store<HexBytes, HexBytes> contractStore) {
        return new PrefixStore<>(
                contractStore,
                VOTE_INFO_KEY,
                Codecs.HEX,
                Codecs.newRLPCodec(VoteInfo.class)
        );
    }

    @Override
    public Account getGenesisAccount() {
        return Account.emptyAccount(Constants.POS_CONTRACT_ADDR, Uint256.ZERO);
    }

    @Override
    public Map<HexBytes, HexBytes> getGenesisStorage() {
        Map<HexBytes, HexBytes> map = new HashMap<>();
        List<NodeInfo> nodeInfos = new ArrayList<>(this.nodes.values());
        nodeInfos.sort(NodeInfo::compareTo);
        map.put(NODE_INFO_KEY, RLPUtil.encode(nodeInfos));
        map.put(VOTE_INFO_KEY, RLPUtil.encode(RLPList.createEmpty()));
        return map;
    }

    @Override
    @SneakyThrows
    public byte[] call(Backend backend, CallData callData) {
        HexBytes payload = callData.getData();
        Type type = Type.values()[payload.get(0)];
        HexBytes args = payload.slice(1);

        List<NodeInfo> nodeInfos = new ArrayList<>(
                Arrays.asList(
                        RLPUtil.decode(backend.dbGet(Constants.POS_CONTRACT_ADDR, NODE_INFO_KEY), NodeInfo[].class))
        );

        PrefixStore<HexBytes, VoteInfo> voteInfos = getVoteInfoStore(backend.getAsStore(Constants.POS_CONTRACT_ADDR));

        switch (type) {
            case VOTE: {
                if (callData.getValue().compareTo(Uint256.ZERO) == 0)
                    throw new RuntimeException("amount of vote cannot be 0 ");
                Map.Entry<Integer, NodeInfo> e =
                        findFirst(nodeInfos, x -> x.address.equals(args));

                NodeInfo n = e.getKey() < 0 ? new NodeInfo(args, Uint256.ZERO, new ArrayList<>()) :
                        e.getValue();

                n.vote = n.vote.safeAdd(callData.getValue());
                n.txHash.add(callData.getTxHash());
                if (e.getKey() < 0)
                    nodeInfos.add(n);
                else
                    nodeInfos.set(e.getKey(), n);
                voteInfos.put(callData.getTxHash(), new VoteInfo(
                        callData.getTxHash(), callData.getCaller(),
                        args, callData.getValue()
                ));
                break;
            }
            case CANCEL_VOTE:
                if (callData.getValue().compareTo(Uint256.ZERO) != 0)
                    throw new RuntimeException("amount of cancel vote should be 0");
                Optional<VoteInfo> o = Optional.ofNullable(voteInfos.get(args));
                voteInfos.remove(args);

                if (!o.isPresent()) {
                    throw new RuntimeException(args + " voting business does not exist and cannot be withdrawn");
                }
                VoteInfo voteInfo = o.get();
                if (!voteInfo.getFrom().equals(callData.getCaller())) {
                    throw new RuntimeException("vote transaction from " + voteInfo.getFrom() + " not equals to " + callData.getCaller());
                }

                Map.Entry<Integer, NodeInfo> e2 =
                        findFirst(nodeInfos, x -> x.address.equals(voteInfo.to));

                if (e2.getKey() < 0) {
                    throw new RuntimeException(voteInfo.getTo() + " abnormal withdrawal of vote");
                }

                NodeInfo ninfo = e2.getValue();

                if (!ninfo.txHash.contains(args))
                    throw new RuntimeException("vote " + args + " not exists");
                ninfo.vote = ninfo.vote.safeSub(voteInfo.amount);
                ninfo.txHash.remove(args);

                if (ninfo.vote.compareTo(Uint256.ZERO) == 0) {
                    nodeInfos.remove((int) e2.getKey());
                } else {
                    nodeInfos.set(e2.getKey(), ninfo);
                }


                Uint256 callerBalance = backend.getBalance(callData.getCaller());
                backend.setBalance(callData.getCaller(), callerBalance.add(voteInfo.amount));

                Uint256 thisBalance = backend.getBalance(Constants.POS_CONTRACT_ADDR);
                backend.setBalance(Constants.POS_CONTRACT_ADDR, thisBalance.safeSub(voteInfo.amount));
                break;
        }
        nodeInfos.sort(NodeInfo::compareTo);
        Collections.reverse(nodeInfos);
        backend.dbSet(Constants.POS_CONTRACT_ADDR, NODE_INFO_KEY, RLPUtil.encode(nodeInfos));
        return ByteUtil.EMPTY_BYTE_ARRAY;
    }

    @Override
    public Abi getAbi() {
        return Abi.fromJson("[]");
    }

    public enum Type {
        VOTE,
        CANCEL_VOTE
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NodeInfo implements Comparable<NodeInfo> {
        private HexBytes address;
        private Uint256 vote;
        private List<HexBytes> txHash;

        @Override
        public int compareTo(NodeInfo o) {
            return vote.compareTo(o.vote);
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VoteInfo implements Comparable<VoteInfo> {
        private HexBytes txHash;
        private HexBytes from;
        private HexBytes to;
        private Uint256 amount;

        @Override
        public int compareTo(VoteInfo o) {
            return txHash.compareTo(o.txHash);
        }
    }
}

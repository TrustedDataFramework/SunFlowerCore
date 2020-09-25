package org.tdf.sunflower.consensus.pos;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.tdf.common.serialize.Codec;
import org.tdf.common.serialize.Codecs;
import org.tdf.common.store.PrefixStore;
import org.tdf.common.store.Store;
import org.tdf.common.types.Uint256;
import org.tdf.common.util.ByteArrayMap;
import org.tdf.common.util.HexBytes;
import org.tdf.rlp.RLPCodec;
import org.tdf.rlp.RLPList;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.AccountTrie;
import org.tdf.sunflower.state.Constants;
import org.tdf.sunflower.state.PreBuiltContract;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.types.Transaction;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j(topic = "pos")
public class PosPreBuilt implements PreBuiltContract {
    public static final byte[] NODE_INFO_KEY = "nodes".getBytes(StandardCharsets.US_ASCII);
    public static final byte[] VOTE_INFO_KEY = "votes".getBytes(StandardCharsets.US_ASCII);

    private Map<HexBytes, NodeInfo> nodes;

    private static <T> Map.Entry<Integer, T> findFirst(List<? extends T> c, Predicate<T> predicate) {
        for (int i = 0; i < c.size(); i++) {
            if (predicate.test(c.get(i))) {
                return new AbstractMap.SimpleImmutableEntry<>(i, c.get(i));
            }
        }
        return new AbstractMap.SimpleImmutableEntry<>(-1, null);
    }

    public enum Type {
        VOTE,
        CANCEL_VOTE
    }

    @Setter
    private AccountTrie accountTrie;

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

    public PosPreBuilt(@NonNull Map<HexBytes, NodeInfo> nodes) {
        this.nodes = nodes;
    }

    private byte[] getValue(byte[] stateRoot, byte[] key) {
        Account a = accountTrie.get(stateRoot, Constants.POS_CONTRACT_ADDR).get();
        Store<byte[], byte[]> db = accountTrie.getContractStorageTrie().revert(a.getStorageRoot());
        return db.get(key).get();
    }

    public List<HexBytes> getNodes(byte[] stateRoot) {
        byte[] v = getValue(stateRoot, NODE_INFO_KEY);
        return Arrays.stream(RLPCodec.decode(v, NodeInfo[].class)).map(NodeInfo::getAddress).collect(Collectors.toList());
    }

    public List<NodeInfo> getNodeInfos(byte[] stateRoot){
        byte[] v = getValue(stateRoot, NODE_INFO_KEY);
        return Arrays.asList(RLPCodec.decode(
                v, NodeInfo[].class));
    }

    public Optional<VoteInfo> getVoteInfo(byte[] stateRoot, byte[] txHash){
        Account a = accountTrie.get(stateRoot, Constants.POS_CONTRACT_ADDR).get();
        Store<byte[], byte[]> db = accountTrie.getContractStorageTrie().revert(a.getStorageRoot());
        PrefixStore<byte[], VoteInfo> store = getVoteInfoStore(db);
        return store.get(txHash);
    }
    public PrefixStore<byte[], VoteInfo> getVoteInfoStore(Store<byte[], byte[]> contractStore){
        return new PrefixStore<>(
                contractStore,
                VOTE_INFO_KEY,
                Codec.identity(),
                Codecs.newRLPCodec(VoteInfo.class)
        );
    }

    @Override
    public Account getGenesisAccount() {
        return Account.emptyContract(Constants.POS_CONTRACT_ADDR);
    }

    @Override
    public Map<byte[], byte[]> getGenesisStorage() {
        Map<byte[], byte[]> map = new ByteArrayMap<>();
        List<NodeInfo> nodeInfos = new ArrayList<>(this.nodes.values());
        nodeInfos.sort(NodeInfo::compareTo);
        map.put(NODE_INFO_KEY, RLPCodec.encode(nodeInfos));
        map.put(VOTE_INFO_KEY, RLPList.createEmpty().getEncoded());
        return map;
    }

    @Override
    @SneakyThrows
    public void update(Header header, Transaction transaction, Map<HexBytes, Account> accounts, Store<byte[], byte[]> contractStorage) {
        Type type = Type.values()[transaction.getPayload().get(0)];
        HexBytes args = transaction.getPayload().slice(1);

        List<NodeInfo> nodeInfos = new ArrayList<>(
                Arrays.asList(
                        RLPCodec.decode(contractStorage.get(NODE_INFO_KEY).get(), NodeInfo[].class))
        );

        PrefixStore<byte[], VoteInfo> voteInfos = getVoteInfoStore(contractStorage);

        switch (type) {
            case VOTE: {
                if(transaction.getAmount().compareTo(Uint256.ZERO) == 0)
                    throw new RuntimeException("amount of vote cannot be 0 ");
                Map.Entry<Integer, NodeInfo> e =
                        findFirst(nodeInfos, x -> x.address.equals(args));

                NodeInfo n = e.getKey() < 0 ? new NodeInfo(args, Uint256.ZERO, new ArrayList<>()) :
                        e.getValue();

                n.vote = n.vote.safeAdd(transaction.getAmount());
                n.txHash.add(transaction.getHash());
                if (e.getKey() < 0)
                    nodeInfos.add(n);
                else
                    nodeInfos.set(e.getKey(), n);
                voteInfos.put(transaction.getHash().getBytes(), new VoteInfo(
                        transaction.getHash(), transaction.getFromAddress(),
                        args, transaction.getAmount()
                ));
                break;
            }
            case CANCEL_VOTE:
                if(transaction.getAmount().compareTo(Uint256.ZERO) != 0)
                    throw new RuntimeException("amount of cancel vote should be 0");
                Optional<VoteInfo> o = voteInfos.get(args.getBytes());
                voteInfos.remove(args.getBytes());

                if (!o.isPresent()) {
                    throw new RuntimeException(args + " voting business does not exist and cannot be withdrawn");
                }
                VoteInfo voteInfo = o.get();
                if (!voteInfo.getFrom().equals(transaction.getFromAddress())) {
                    throw new RuntimeException("vote transaction from " + voteInfo.getFrom() + " not equals to " + transaction.getFromAddress());
                }

                Map.Entry<Integer, NodeInfo> e2 =
                        findFirst(nodeInfos, x -> x.address.equals(voteInfo.to));

                if (e2.getKey() < 0) {
                    throw new RuntimeException(voteInfo.getTo() + " abnormal withdrawal of vote");
                }

                NodeInfo ninfo = e2.getValue();

                if(!ninfo.txHash.contains(args))
                    throw new RuntimeException("vote " + args + " not exists");
                ninfo.vote = ninfo.vote.safeSub(voteInfo.amount);
                ninfo.txHash.remove(args);

                if (ninfo.vote.compareTo(Uint256.ZERO) == 0) {
                    nodeInfos.remove((int) e2.getKey());
                } else {
                    nodeInfos.set(e2.getKey(), ninfo);
                }

                Account fromaccount = accounts.get(transaction.getFromAddress());
                fromaccount.addBalance(voteInfo.getAmount());
                accounts.put(fromaccount.getAddress(), fromaccount);
                Account thisContract = accounts.get(Constants.POS_CONTRACT_ADDR);
                thisContract.subBalance(voteInfo.amount);
                accounts.put(thisContract.getAddress(), thisContract);
                break;
        }
        nodeInfos.sort(NodeInfo::compareTo);
        Collections.reverse(nodeInfos);
        contractStorage.put(NODE_INFO_KEY, RLPCodec.encode(nodeInfos));
    }
}

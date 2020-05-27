package org.tdf.sunflower.consensus.pos;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.tdf.common.store.Store;
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
    public static final byte[] NODEINFO_KEY = "nodekey".getBytes(StandardCharsets.US_ASCII);
    public static final byte[] VOTEINFO_KEY = "trankey".getBytes(StandardCharsets.US_ASCII);

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
        Vote,
        CancelVote
    }

    @Setter
    private AccountTrie accountTrie;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NodeInfo implements Comparable<NodeInfo> {
        private HexBytes node;
        private long vote;

        @Override
        public int compareTo(NodeInfo o) {
            return Long.compare(vote, o.vote);
        }


    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VoteInfo implements Comparable<VoteInfo> {
        private HexBytes txhash;
        private HexBytes from;
        private HexBytes to;
        private Long amount;

        @Override
        public int compareTo(VoteInfo o) {
            return txhash.compareTo(o.txhash);
        }
    }

    public PosPreBuilt(@NonNull Map<HexBytes, NodeInfo> nodes) {
        this.nodes = nodes;
    }

    private byte[] getValue(byte[] stateRoot, byte[] key) {
        Account a = accountTrie.get(stateRoot, Constants.POS_AUTHENTICATION_ADDR).get();
        Store<byte[], byte[]> db = accountTrie.getContractStorageTrie().revert(a.getStorageRoot());
        return db.get(key).get();
    }

    public List<HexBytes> getNodes(byte[] stateRoot) {
        byte[] v = getValue(stateRoot, NODEINFO_KEY);
        return Arrays.stream(RLPCodec.decode(v, NodeInfo[].class)).map(NodeInfo::getNode).collect(Collectors.toList());
    }


    @Override
    public Account getGenesisAccount() {
        return Account.emptyContract(Constants.POS_AUTHENTICATION_ADDR);
    }

    @Override
    public Map<byte[], byte[]> getGenesisStorage() {
        Map<byte[], byte[]> map = new ByteArrayMap<>();
        List<NodeInfo> nodeInfos = new ArrayList<>(this.nodes.values());
        nodeInfos.sort(NodeInfo::compareTo);
        map.put(NODEINFO_KEY, RLPCodec.encode(nodeInfos));
        map.put(VOTEINFO_KEY, RLPList.createEmpty().getEncoded());
        return map;
    }

    @Override
    @SneakyThrows
    public void update(Header header, Transaction transaction, Map<HexBytes, Account> accounts, Store<byte[], byte[]> contractStorage) {
        Type type = Type.values()[transaction.getPayload().get(0)];
        HexBytes args = transaction.getPayload().slice(1);
        List<NodeInfo> nodeInfos = new ArrayList<>(Arrays.asList(RLPCodec.decode(
                contractStorage.get(NODEINFO_KEY).get(), NodeInfo[].class)));

        List<VoteInfo> voteInfos = new ArrayList<>(Arrays.asList(RLPCodec.decode(
                contractStorage.get(VOTEINFO_KEY).get(),
                VoteInfo[].class)));

        switch (type) {
            case Vote: {
                Map.Entry<Integer, NodeInfo> e =
                        findFirst(nodeInfos, x -> x.node.equals(args));

                NodeInfo n = e.getKey() < 0 ? new NodeInfo(args, 0) :
                        e.getValue();

                n.vote += transaction.getAmount();
                if (e.getKey() < 0)
                    nodeInfos.add(n);
                else
                    nodeInfos.set(e.getKey(), n);
                voteInfos.add(new VoteInfo(
                        transaction.getHash(), transaction.getFromAddress(),
                        args, transaction.getAmount()
                ));
                break;
            }
            case CancelVote:
                Map.Entry<Integer, VoteInfo> e =
                        findFirst(voteInfos, x -> x.txhash.equals(args));

                if (e.getKey() < 0) {
                    throw new RuntimeException(args + " voting business does not exist and cannot be withdrawn");
                }
                VoteInfo voteInfo = voteInfos.get(e.getKey());
                if (!voteInfo.getFrom().equals(transaction.getFromAddress())) {
                    throw new RuntimeException("vote transaction from " + voteInfo.getFrom() + " not equals to " + transaction.getFromAddress());
                }

                voteInfos.remove((int) e.getKey());

                Map.Entry<Integer, NodeInfo> e2 =
                        findFirst(nodeInfos, x -> x.node.equals(voteInfo.to));

                if (e2.getKey() < 0) {
                    throw new RuntimeException(voteInfo.getTo() + " abnormal withdrawal of vote");
                }

                NodeInfo ninfo = e2.getValue();
                ninfo.vote -= voteInfo.amount;
                if (ninfo.vote == 0) {
                    nodeInfos.remove((int) e2.getKey());
                } else {
                    nodeInfos.set(e2.getKey(), ninfo);
                }

                Account fromaccount = accounts.get(transaction.getFromAddress());
                fromaccount.setBalance(fromaccount.getBalance() + voteInfo.getAmount());
                accounts.put(fromaccount.getAddress(), fromaccount);
                Account thisContract = accounts.get(Constants.POS_AUTHENTICATION_ADDR);
                thisContract.setBalance(thisContract.getBalance() - voteInfo.amount);
                break;
        }
        nodeInfos.sort(NodeInfo::compareTo);
        voteInfos.sort(VoteInfo::compareTo);
        Collections.reverse(nodeInfos);
        contractStorage.put(VOTEINFO_KEY, RLPCodec.encode(voteInfos));
        contractStorage.put(NODEINFO_KEY, RLPCodec.encode(nodeInfos));
    }
}

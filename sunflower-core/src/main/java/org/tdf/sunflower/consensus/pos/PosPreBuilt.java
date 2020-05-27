package org.tdf.sunflower.consensus.pos;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.tdf.common.store.Store;
import org.tdf.common.util.ByteArrayMap;
import org.tdf.common.util.HexBytes;
import org.tdf.rlp.Container;
import org.tdf.rlp.RLPCodec;
import org.tdf.rlp.RLPList;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.AccountTrie;
import org.tdf.sunflower.state.Constants;
import org.tdf.sunflower.state.PreBuiltContract;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.types.Transaction;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

@Slf4j(topic = "pos")
public class PosPreBuilt implements PreBuiltContract {
    public static final byte[] NODEINFO_KEY = "nodekey".getBytes(StandardCharsets.US_ASCII);
    public static final byte[] VOTEINFO_KEY = "trankey".getBytes(StandardCharsets.US_ASCII);

    private Map<HexBytes, NodeInfo> nodes;

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
            if (this.vote < o.getVote()) {
                return -1;
            } else if (this.vote > o.getVote()) {
                return 1;
            }
            return 0;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VoteInfo {
        private HexBytes txhash;
        private HexBytes from;
        private HexBytes to;
        private Long amount;
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
        return Arrays.asList(RLPCodec.decode(v, HexBytes[].class));
    }

    @Override
    public Account getGenesisAccount() {
        return Account.emptyContract(Constants.POS_AUTHENTICATION_ADDR);
    }

    @Override
    public Map<byte[], byte[]> getGenesisStorage() {
        Map<byte[], byte[]> map = new ByteArrayMap<>();
        map.put(NODEINFO_KEY, RLPCodec.encode(this.nodes));
        map.put(VOTEINFO_KEY, RLPList.createEmpty().getEncoded());
        return map;
    }

    @Override
    @SneakyThrows
    public void update(Header header, Transaction transaction, Map<HexBytes, Account> accounts, Store<byte[], byte[]> contractStorage) {
        Type type = Type.values()[Byte.toUnsignedInt(transaction.getPayload().getBytes()[0])];
        HexBytes args = transaction.getPayload().slice(1, transaction.getPayload().size() - 1);
        Map<HexBytes, NodeInfo> nodeInfoMap = (Map<HexBytes, NodeInfo>) RLPCodec.decodeContainer(
                contractStorage.get(NODEINFO_KEY).get(),
                Container.fromField(PosPreBuilt.class.getField("nodes")));
        Map<HexBytes, VoteInfo> voteInfoMap = Arrays.asList(RLPCodec.decode(
                contractStorage.get(VOTEINFO_KEY).get(),
                VoteInfo[].class)).stream().collect(toMap(t -> t.getTxhash(), t -> t));
        long votes = 0;
        NodeInfo nodeInfo;
        Map<HexBytes, NodeInfo> sortedMap = new LinkedHashMap<>();
        switch (type) {
            case Vote:
                if (!accounts.containsKey(transaction.getFromAddress())) {
                    throw new RuntimeException(transaction.getFromAddress() + " insufficient balance from account");
                }
                Account account = accounts.get(transaction.getFromAddress());
                long nonce = account.getNonce();
                nonce++;
                account.setNonce(nonce);
                long balance = account.getBalance();
                balance -= transaction.getAmount();
                account.setBalance(balance);
                accounts.put(transaction.getFromAddress(), account);
                if (nodeInfoMap.containsKey(args)) {
                    nodeInfo = nodeInfoMap.get(args);
                    votes = nodeInfo.getVote();
                    votes += transaction.getAmount();
                    nodeInfo.setVote(votes);
                } else {
                    votes += transaction.getAmount();
                    nodeInfo = new NodeInfo(args, votes);
                }
                voteInfoMap.put(transaction.getHash(), new VoteInfo(
                        transaction.getHash(), transaction.getFromAddress(),
                        args, transaction.getAmount()
                ));
                nodeInfoMap.put(args, nodeInfo);
                break;
            case CancelVote:
                if (!voteInfoMap.containsKey(args)) {
                    throw new RuntimeException(args + " voting business does not exist and cannot be withdrawn");
                }
                VoteInfo voteInfo = voteInfoMap.get(args);
                if (!voteInfo.getTxhash().equals(args) || !voteInfo.getFrom().equals(transaction.getFromAddress())
                        || voteInfo.amount != transaction.getAmount()) {
                    throw new RuntimeException(args + " it doesnot match");
                }
                voteInfoMap.remove(args);
                if (!nodeInfoMap.containsKey(voteInfo.getTo())) {
                    throw new RuntimeException(voteInfo.getTo() + " abnormal withdrawal of vote");
                }
                NodeInfo ninfo = nodeInfoMap.get(voteInfo.getTo());
                votes = ninfo.getVote();
                votes -= transaction.getAmount();
                if (votes <= 0) {
                    nodeInfoMap.remove(voteInfo.getTo());
                } else {
                    ninfo.setVote(votes);
                    nodeInfoMap.put(voteInfo.getTo(), ninfo);
                }

                if (!accounts.containsKey(transaction.getFromAddress())) {
                    throw new RuntimeException(transaction.getFromAddress() + " insufficient balance from account");
                }
                Account fromaccount = accounts.get(transaction.getFromAddress());
                long fromnonce = fromaccount.getNonce();
                fromnonce++;
                fromaccount.setNonce(fromnonce);
                long frombalance = fromaccount.getBalance();
                frombalance += transaction.getAmount();
                fromaccount.setBalance(frombalance);
                accounts.put(transaction.getFromAddress(), fromaccount);
                break;
        }
        contractStorage.put(VOTEINFO_KEY, RLPCodec.encode(voteInfoMap));
        nodeInfoMap.entrySet().stream()
                .sorted(Map.Entry.<HexBytes, NodeInfo>comparingByValue().reversed())
                .forEachOrdered(e -> sortedMap.put(e.getKey(), e.getValue()));
        contractStorage.put(NODEINFO_KEY, RLPCodec.encode(sortedMap));
    }
}

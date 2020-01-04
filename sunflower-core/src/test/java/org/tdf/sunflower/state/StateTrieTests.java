package org.tdf.sunflower.state;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.codec.binary.Hex;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.tdf.common.serialize.Codec;
import org.tdf.common.serialize.Codecs;
import org.tdf.common.trie.Trie;
import org.tdf.common.util.HexBytes;
import org.tdf.rlp.RLPCodec;
import org.tdf.sunflower.DatabaseConfig;
import org.tdf.sunflower.db.DatabaseStoreFactory;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.Transaction;

import java.util.*;
import java.util.stream.Collectors;

@RunWith(JUnit4.class)
public class StateTrieTests {
    private static final HexBytes ADDRESS_A = HexBytes.fromBytes(new byte[]{0x0a});
    private static final HexBytes ADDRESS_B = HexBytes.fromBytes(new byte[]{0x0b});
    private static final HexBytes ADDRESS_C = HexBytes.fromBytes(new byte[]{0x0c});

    // account to test
    @AllArgsConstructor
    @Getter
    @Setter
    @NoArgsConstructor
    public static class Account {
        private String address;
        private long balance;

        public Account clone() {
            return new Account(address, balance);
        }
    }

    public static class AccountUpdater extends AbstractStateUpdater<String, Account> {
        @Override
        public Map<String, Account> getGenesisStates() {
            Map<String, Account> ret = createEmptyMap();
            Arrays.asList(new Account(ADDRESS_A.toString(), 100),
                    new Account(ADDRESS_B.toString(), 100))
                    .forEach(a -> ret.put(a.address, a));
            return ret;
        }

        @Override
        public Set<String> getRelatedKeys(Transaction transaction) {
            return new HashSet<>(Arrays.asList(transaction.getFrom().toString(), transaction.getTo().toString()));
        }

        @Override
        public Account update(String s, Account state, Transaction t) {
            state = state.clone();
            if (t.getFrom().toString().equals(state.address)) {
                state.balance -= t.getAmount();
            }
            if (t.getTo().toString().equals(state.address)) {
                state.balance += t.getAmount();
            }
            return state;
        }

        @Override
        public Account createEmpty(String s) {
            return new Account(s, 0);
        }

        @Override
        public Map<String, Account> createEmptyMap() {
            return new HashMap<>();
        }

        @Override
        public Set<String> createEmptySet() {
            return new HashSet<>();
        }
    }

    public static class AccountStateTrie extends AbstractStateTrie<String, Account> {
        private Map<HexBytes, byte[]> roots = new HashMap<>();

        public AccountStateTrie(
                Block genesis, DatabaseStoreFactory factory) {
            super(new AccountUpdater(),
                    Codecs.STRING,
                    Codec.newInstance(RLPCodec::encode, x -> RLPCodec.decode(x, Account.class)),
                    factory, false);
            roots.put(genesis.getHash(), getGenesisRoot());
        }

        @Override
        protected String getPrefix() {
            return "";
        }

        @Override
        public void commit(Block block) {
            byte[] parentRoot = roots.getOrDefault(block.getHashPrev(), getTrie().getNullHash());
            Trie<String, Account> trie = getTrie().revert(parentRoot);
            Map<String, Account> map = getUpdater().createEmptyMap();
            getUpdater().getRelatedKeys(block)
                    .forEach(k -> {
                        map.put(k, trie.get(k).orElse(getUpdater().createEmpty(k)));
                    });
            byte[] root = commitInternal(parentRoot, getUpdater().update(map, block)).getRootHash();
            roots.put(block.getHash(), root);
        }

        // get an optional state at a block
        Optional<Account> getByBlockHash(byte[] blockHash, String id){
            return get(roots.get(HexBytes.fromBytes(blockHash)), id);
        }
    }

    protected AccountStateTrie stateTrie;

    protected Map<String, Block> blocks;

    protected DatabaseStoreFactory databaseStoreFactory;

    @Before
    public void before() throws Exception {
        DatabaseConfig config = new DatabaseConfig();
        config.setName("memory");
        databaseStoreFactory = new DatabaseStoreFactory(config);
        blocks = StateFactoryTests.getBlocks().stream()
                .collect(Collectors.toMap(
                        b -> b.getHash().toString(),
                        b -> b
                ));
        blocks.get("0002").getBody().add(Transaction.builder().from(ADDRESS_A).to(ADDRESS_B).amount(50).build());
        blocks.get("0102").getBody().add(Transaction.builder().from(ADDRESS_A).to(ADDRESS_B).amount(60).build());

        stateTrie = new AccountStateTrie(blocks.get("0000"), databaseStoreFactory);

        blocks.values().stream()
                .filter(x -> x.getHeight() != 0)
                .sorted(Comparator.comparingLong(Block::getHeight)).forEach(stateTrie::commit);

    }

    @Test
    public void test0() throws Exception{
        Optional<Account> o = stateTrie
                .getByBlockHash(Hex.decodeHex("0002".toCharArray()), ADDRESS_A.toString());
        assert o.isPresent();
        assert o.get().balance == 50;
        o = stateTrie
                .getByBlockHash(Hex.decodeHex("0002".toCharArray()), ADDRESS_B.toString());
        assert o.isPresent();
        assert o.get().balance == 150;
        o = stateTrie
                .getByBlockHash(Hex.decodeHex("0102".toCharArray()), ADDRESS_A.toString());
        assert o.isPresent();
        assert o.get().balance == 40;
        o = stateTrie
                .getByBlockHash(Hex.decodeHex("0102".toCharArray()), ADDRESS_B.toString());
        assert o.isPresent();
        assert o.get().balance == 160;
        o = stateTrie
                .getByBlockHash(Hex.decodeHex("0105".toCharArray()), ADDRESS_B.toString());
        assert o.isPresent();
        assert o.get().balance == 160;
    }
}

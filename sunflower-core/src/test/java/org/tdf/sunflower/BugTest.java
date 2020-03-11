package org.tdf.sunflower;

import org.springframework.beans.factory.annotation.Qualifier;
import org.tdf.common.serialize.Codec;
import org.tdf.common.store.NoDeleteBatchStore;
import org.tdf.common.store.Store;
import org.tdf.common.trie.Trie;
import org.tdf.common.util.HexBytes;
import org.tdf.gmhelper.SM3Util;
import org.tdf.sunflower.db.DatabaseStoreFactory;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.AccountTrie;
import org.tdf.sunflower.state.AccountUpdater;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.types.Transaction;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicLong;

public class BugTest {
    private static DatabaseStoreFactory factory =
            new DatabaseStoreFactory(new DatabaseConfig("memory", 0, "", false, ""));

    @Qualifier("contractCodeStore")
    private static Store<byte[], byte[]> contractStore = factory.create("contractCodeStore");


    private static NoDeleteBatchStore<byte[], byte[]> db = new NoDeleteBatchStore<>(factory.create("contract-db"));

    @Qualifier("contractStorageTrie")
    private static Trie<byte[], byte[]> storageTrie = Trie.<byte[], byte[]>builder()
            .hashFunction(SM3Util::hash)
            .keyCodec(Codec.identity())
            .valueCodec(Codec.identity())
            .store(db)
            .build();

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public static void main(String[] args) {
        String[] pks = new String[]{
                "02b507fe1afd0cc7a525488292beadbe9f143784de44f8bc1c991636509fd50936",
                "02f9d915954e04107d11fb9689a6330c22199e1e830857bff076e033bbca2888d4",
//                "03cd2875d6381b974bd13c9c6087c08fcf0b9b700ea8c9b601ae35a6a9651fbce2",
//                "03744f32e35e8e45cfa6360b49fe12e730cb294bce40db5099b0de697aa00a3d71",
//                "020f0a0c7fb839a51b1f64cf2b49f9b3269d2b9ca49d3309e3de4453f389c827bb",
//                "02281b065a508bace266556f239e491a44a0a64789c7d4fc333d2a875a7ddc1714",
//                "039608d098f275db24db04211d62f66e1438bdd0d93d7e3dbe33136ef3fc53c726"
        };
        int iters = 100;
        int count = 100;
        String[] addresses = new String[]{"9cbf30db111483e4b84e77ca0e39378fd7605e1b",
                "bf0aba026e5a0e1a69094c8a0d19d905367d64cf",
                "54e670985631117904d10341676a57d11687bbc5",
                "23142c9d93b20d93663fb4327f598af6e2e71850",
                "5cc87525535e1668599373ad2ac475226beebede",
                "5438264bcc5a2a4a237f96fe57a7e0dcfc380ece",
                "3b776ac5d6f414c0d841f23c7e719d6eba52f95e"};

        Map<String, AtomicLong> nonceCounter = new HashMap<>();
        ApplicationConstants.PUBLIC_KEY_SIZE = 33;

        for (String pk : pks) {
            nonceCounter.put(pk, new AtomicLong());
        }

        Map<HexBytes, Account> alloc = new HashMap<>();
        for (String addr : addresses) {
            alloc.put(HexBytes.fromHex(addr), new Account(HexBytes.fromHex(addr), 10000000));
        }
        AccountUpdater updater = new AccountUpdater(alloc, contractStore, storageTrie);
        AccountTrie accountTrie = new AccountTrie(updater, factory, contractStore, storageTrie);

        List<byte[]> roots = new ArrayList<>();
        for (String s : addresses) {
            roots.add(accountTrie.getGenesisRoot().getBytes());
        }

        for (int i = 0; i < iters; i++) {
            List<Transaction> txs = new ArrayList<>(addresses.length * count);
            CompletableFuture[] futures = new CompletableFuture[pks.length];
            for (int pi = 0; pi < pks.length; pi++) {
                int finalPi = pi;
                CompletableFuture<?> future = CompletableFuture.runAsync(() -> {
                    for (int j = 0; j < count; j++) {
                        HexBytes from = HexBytes.fromHex(pks[finalPi]);
                        byte[] toBytes = new byte[20];
                        SECURE_RANDOM.nextBytes(toBytes);
                        Transaction tx = Transaction
                                .builder().amount(1).from(from).to(HexBytes.fromBytes(toBytes))
                                .type(Transaction.Type.TRANSFER.code)
                                .nonce(nonceCounter.get(pks[finalPi]).incrementAndGet())
                                .build();
                        txs.add(tx);
                    }
                    Block b = new Block(new Header());
                    b.setBody(txs);
                    Trie<?, ?> t = accountTrie.update(roots.get(finalPi), b);
                    roots.set(finalPi, t.commit());
                    t.flush();
                });
                futures[pi] = future;
            }
            CompletableFuture.allOf(futures).join();
        }
    }
}


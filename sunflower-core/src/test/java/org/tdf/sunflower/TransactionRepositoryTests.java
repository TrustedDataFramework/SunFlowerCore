package org.tdf.sunflower;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.tdf.common.util.BigEndian;
import org.tdf.sunflower.service.BlockRepositoryService;
import org.tdf.sunflower.service.TransactionRepositoryService;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.Transaction;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.tdf.sunflower.TestUtils.BYTES;
import static org.tdf.sunflower.TestUtils.getBlock;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestContext.class)
// use SPRING_CONFIG_LOCATION environment to locate spring config
// for example: SPRING_CONFIG_LOCATION=classpath:\application.yml,some-path\custom-config.yml
// set consensus.name = 'none' to run this test class
public class TransactionRepositoryTests {
    @Autowired
    private TransactionRepositoryService transactionStore;

    @Autowired
    private BlockRepositoryService blockStore;

    private void assertTransaction(Transaction transaction) {
        String h = Long.toString(BigEndian.decodeInt64(transaction.getBlockHash().getBytes()));
        assert Long.toString(transaction.getHeight()).equals(h);
        assert new String(transaction.getHash().getBytes()).startsWith(h);
        assert transaction.getVersion() == 0;
        assert Arrays.equals(transaction.getFrom().getBytes(), BYTES) &&
                Arrays.equals(transaction.getPayload().getBytes(), BYTES) &&
                Arrays.equals(transaction.getTo().getBytes(), BYTES) &&
                Arrays.equals(transaction.getSignature().getBytes(), BYTES);
    }

    @Before
    public void saveBlocks() {
        if (blockStore.getBlockByHeight(0).isPresent()) {
            return;
        }
        for (int i = 0; i < 10; i++) {
            Block b = getBlock(i);
            assert !blockStore.containsBlock(b.getHash().getBytes());
            blockStore.writeBlock(b);
        }
    }

    @Test
    public void test() {
        assert transactionStore != null;
        assert blockStore != null;
    }

    @Test
    public void testHasTransaction() {
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 3; j++) {
                assert transactionStore.containsTransaction((i + "" + j).getBytes());
            }
        }
        assert !transactionStore.containsTransaction((1 + "" + 1000).getBytes());
    }

    @Test
    public void testGetTransactionsByBlockHash() {
        assert transactionStore.getTransactionsByBlockHash(BigEndian.encodeInt64(0)).size() == 3;
        assert transactionStore.getTransactionsByBlockHash("-1".getBytes()).size() == 0;
    }

    @Test
    public void testGetTransactionsByBlockHeight() {
        assert transactionStore.getTransactionsByBlockHeight(0).size() == 3;
        assert transactionStore.getTransactionsByBlockHeight(-1).size() == 0;
    }
}

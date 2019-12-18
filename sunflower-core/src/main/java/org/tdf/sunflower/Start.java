package org.tdf.sunflower;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.util.Assert;
import org.tdf.sunflower.consensus.poa.PoA;
import org.tdf.sunflower.consensus.vrf.VrfEngine;
import org.tdf.sunflower.db.DatabaseStoreFactory;
import org.tdf.sunflower.facade.*;
import org.tdf.sunflower.mq.MessageQueue;
import org.tdf.sunflower.mq.SocketIOMessageQueue;
import org.tdf.sunflower.net.PeerServer;
import org.tdf.sunflower.net.PeerServerImpl;
import org.tdf.sunflower.pool.TransactionPoolImpl;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.ConsortiumStateRepository;
import org.tdf.sunflower.state.InMemoryStateTree;
import org.tdf.sunflower.types.Block;

import java.util.List;
import java.util.Optional;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@EnableAsync
@EnableScheduling
@SpringBootApplication
@EnableTransactionManagement
@Slf4j
// use SPRING_CONFIG_LOCATION environment to locate spring config
// for example: SPRING_CONFIG_LOCATION=classpath:\application.yml,some-path\custom-config.yml
public class Start {
    private static final boolean ENABLE_ASSERTION = "true".equals(System.getenv("ENABLE_ASSERTION"));

    public static final Executor APPLICATION_THREAD_POOL = Executors.newCachedThreadPool();

    public static void devAssert(boolean truth, String error) {
        if (!ENABLE_ASSERTION) return;
        Assert.isTrue(truth, error);
    }

    public static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .enable(JsonParser.Feature.ALLOW_COMMENTS);

    public static void main(String[] args) {
        SpringApplication.run(Start.class, args);
    }

    @Bean
    public ObjectMapper getObjectMapper() {
        return MAPPER;
    }

    @Bean
    public Miner miner(ConsensusEngine engine, NewMinedBlockWriter writer,
                       // those dependencies ensure transaction pool, consortium repository and peer server
                       // had been initialized and injected before miner start
                       TransactionPool transactionPool, ConsortiumRepository repository, PeerServer peerServer) {
        Miner miner = engine.getMiner();
        miner.addListeners(writer);
        miner.start();
        return miner;
    }

    @Bean
    public StateRepository stateRepository(
            ConsensusEngine engine,
            DatabaseStoreFactory factory,
            ConsortiumRepository consortiumRepository
    ) {
        if (!(engine.getStateRepository() instanceof ConsortiumStateRepository)) return engine.getStateRepository();
        ConsortiumStateRepository repo = (ConsortiumStateRepository) engine.getStateRepository();
        if (!repo.getClasses().contains(Account.class)) {
            return repo;
        }
        // TODO: replace byte array map store with persistent data storage
        InMemoryStateTree<Account> tree = repo.getStateTree(Account.class);
        long current = consortiumRepository.getBlock(tree.getWhere().getBytes()).map(Block::getHeight).orElseThrow(() ->
                new RuntimeException("cannot find state at " + tree.getWhere() + " please clear account db manually")
        );
        Block best = consortiumRepository.getBestBlock();
        int blocksPerUpdate = 4096;
        if (tree.getWhere().equals(best.getHash())) {
            return repo;
        }
        while (true) {
            List<Block> blocks = consortiumRepository.getBlocksBetween(current + 1, current + blocksPerUpdate);
            if (blocks.size() == 0) break;
            log.info("update account state from height {} to {}", blocks.get(0).getHeight(), blocks.get(blocks.size() - 1).getHeight());
            blocks.forEach(x -> {
                tree.update(x);
                tree.confirm(x.getHash().getBytes());
            });
            if (blocks.stream().allMatch(x -> x.getHash().equals(best.getHash()))) {
                break;
            }
            current = blocks.get(blocks.size() - 1).getHeight();
        }
        return repo;
    }

    @Bean
    public PendingTransactionValidator transactionValidator(ConsensusEngine engine) {
        return engine.getValidator();
    }

    @Bean
    public ConsensusEngine consensusEngine(
            ConsensusProperties consensusProperties,
            ConsortiumRepository consortiumRepository,
            TransactionPoolImpl transactionPool
    ) throws Exception {
        String name = consensusProperties.getProperty(ConsensusProperties.CONSENSUS_NAME);
        name = name == null ? "" : name;
        final ConsensusEngine engine;
        switch (name.trim().toLowerCase()) {
            // none consensus selected, used for unit test
            case ApplicationConstants.CONSENSUS_NONE:
                log.warn("none consensus engine selected, please ensure you are in test mode");
                engine = ConsensusEngine.NONE;
                break;
            case ApplicationConstants.CONSENSUS_POA:
                // use poa as default consensus
                // another engine: pow, pos, pow+pos, vrf
                engine = new PoA();
                break;
            case ApplicationConstants.CONSENSUS_VRF:
                // use poa as default consensus
                // another engine: pow, pos, pow+pos, vrf
                engine = new VrfEngine();
                break;
            default:
                log.error(
                        "none available consensus configured by sunflower.consensus.name=" + name +
                                " please provide available consensus engine");
                log.error("roll back to poa consensus");
                engine = new PoA();
        }
        engine.setConsortiumRepository(consortiumRepository);
        engine.setTransactionPool(transactionPool);
        engine.init(consensusProperties);
        consortiumRepository.setProvider(engine.getConfirmedBlocksProvider());
        // register event listeners
        consortiumRepository.addListeners(engine.getStateRepository());
        // None consensus actually has no genesis block
        if(engine == ConsensusEngine.NONE) return engine;
        consortiumRepository.saveGenesisBlock(engine.getGenesisBlock());
        return engine;
    }

    abstract static class NewMinedBlockWriter implements MinerListener {
    }

    // create a miner listener for write block
    @Bean
    public NewMinedBlockWriter newMinedBlockWriter(ConsortiumRepository repository, ConsensusEngine engine) {
        return new NewMinedBlockWriter() {
            @Override
            public void onBlockMined(Block block) {
                Optional<Block> o = repository.getBlock(block.getHashPrev().getBytes());
                if (!o.isPresent()) return;
                if (engine.getValidator().validate(block, o.get()).isSuccess()) {
                    repository.writeBlock(block);
                }
            }

            @Override
            public void onMiningFailed(Block block) {

            }
        };
    }

    // create peer server from properties
    @Bean
    public PeerServer peerServer(
            PeerServerProperties properties,
            ConsensusEngine engine,
            DatabaseStoreFactory factory
    ) throws Exception {
        String name = properties.getProperty("name");
        name = name == null ? "" : name;
        if (name.trim().toLowerCase().equals("none")) {
            return PeerServerImpl.NONE;
        }
        PeerServer peerServer = new PeerServerImpl().withStore(
                factory.create("peers")
        );
        peerServer.init(properties);
        peerServer.use(engine.getPeerServerListener());
        peerServer.start();
        return peerServer;
    }

    // create message queue service
    @Bean
    public MessageQueue messageQueue(MessageQueueConfig config) {
        String name = config.getName().toLowerCase().trim();
        if (name.equals("none")) return MessageQueue.NONE;
        return new SocketIOMessageQueue(config);
    }
}

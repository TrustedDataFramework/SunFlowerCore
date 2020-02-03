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
import org.tdf.common.event.EventBus;
import org.tdf.sunflower.consensus.poa.PoA;
import org.tdf.sunflower.consensus.vrf.VrfEngine;
import org.tdf.sunflower.db.DatabaseStoreFactory;
import org.tdf.sunflower.events.Bridge;
import org.tdf.sunflower.facade.*;
import org.tdf.sunflower.mq.BasicMessageQueue;
import org.tdf.sunflower.mq.SocketIOMessageQueue;
import org.tdf.sunflower.net.PeerServer;
import org.tdf.sunflower.net.PeerServerImpl;
import org.tdf.sunflower.pool.TransactionPoolImpl;
import org.tdf.sunflower.service.SunflowerRepositoryService;
import org.tdf.sunflower.state.*;

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
    public ObjectMapper objectMapper() {
        return MAPPER;
    }

    @Bean
    public Miner miner(
            ConsensusEngineFacade engine,
            Bridge bridge,
            // this dependency asserts the peer server had been initialized
            PeerServer peerServer
    ) {
        Miner miner = engine.getMiner();
        miner.addListeners(bridge);
        miner.start();
        return miner;
    }

    @Bean
    public AccountTrie accountTrie(ConsensusEngineFacade consensusEngine){
        return (AccountTrie) consensusEngine.getAccountTrie();
    }

    @Bean
    public AccountUpdater accountUpdater(AccountTrie accountTrie){
        return (AccountUpdater) accountTrie.getUpdater();
    }

    @Bean
    public ConsensusEngineFacade consensusEngine(
            ConsensusProperties consensusProperties,
            SunflowerRepositoryService repositoryService,
            TransactionPoolImpl transactionPool,
            DatabaseStoreFactory databaseStoreFactory,
            EventBus eventBus
    ) throws Exception {
        String name = consensusProperties.getProperty(ConsensusProperties.CONSENSUS_NAME);
        name = name == null ? "" : name;
        final ConsensusEngineFacade engine;
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
        engine.setSunflowerRepository(repositoryService);
        engine.setTransactionPool(transactionPool);
        engine.setDatabaseStoreFactory(databaseStoreFactory);
        engine.setEventBus(eventBus);

        engine.init(consensusProperties);

        repositoryService.setProvider(engine.getConfirmedBlocksProvider());
        repositoryService.setAccountTrie(engine.getAccountTrie());

        transactionPool.setEngine(engine);
        if (engine == ConsensusEngine.NONE) return engine;
        repositoryService.saveGenesis(engine.getGenesisBlock());
        return engine;
    }

    // create peer server from properties
    @Bean
    public PeerServer peerServer(
            PeerServerProperties properties,
            ConsensusEngineFacade engine,
            DatabaseStoreFactory factory
    ) throws Exception {
        String name = properties.getProperty("name");
        name = name == null ? "" : name;
        if (name.trim().toLowerCase().equals("none")) {
            return PeerServer.NONE;
        }
        PeerServer peerServer = new PeerServerImpl().withStore(
                factory.create("peers")
        );
        peerServer.init(properties);
        peerServer.addListeners(engine.getPeerServerListener());
        peerServer.start();
        return peerServer;
    }

    // create message queue service
    @Bean
    public BasicMessageQueue messageQueue(MessageQueueConfig config) {
        String name = config.getName();
        name = name == null ? "" : name.toLowerCase().trim();
        if (name.equals("none")) return BasicMessageQueue.NONE;
        return new SocketIOMessageQueue(config);
    }

    @Bean
    public EventBus eventBus() {
        return new EventBus();
    }
}

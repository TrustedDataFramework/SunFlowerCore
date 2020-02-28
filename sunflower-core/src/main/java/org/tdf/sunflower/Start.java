package org.tdf.sunflower;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.util.Assert;
import org.tdf.common.event.EventBus;
import org.tdf.crypto.CryptoContext;
import org.tdf.crypto.ed25519.Ed25519;
import org.tdf.crypto.ed25519.Ed25519PrivateKey;
import org.tdf.crypto.ed25519.Ed25519PublicKey;
import org.tdf.crypto.sm2.SM2;
import org.tdf.crypto.sm2.SM2PrivateKey;
import org.tdf.crypto.sm2.SM2PublicKey;
import org.tdf.gmhelper.SM3Util;
import org.tdf.sunflower.consensus.poa.PoA;
import org.tdf.sunflower.consensus.vrf.VrfEngine;
import org.tdf.sunflower.dao.HeaderDao;
import org.tdf.sunflower.dao.TransactionDao;
import org.tdf.sunflower.db.DatabaseStoreFactory;
import org.tdf.sunflower.exception.ApplicationException;
import org.tdf.sunflower.facade.ConsensusEngine;
import org.tdf.sunflower.facade.ConsensusEngineFacade;
import org.tdf.sunflower.facade.Miner;
import org.tdf.sunflower.facade.SunflowerRepository;
import org.tdf.sunflower.mq.BasicMessageQueue;
import org.tdf.sunflower.mq.SocketIOMessageQueue;
import org.tdf.sunflower.net.PeerServer;
import org.tdf.sunflower.net.PeerServerImpl;
import org.tdf.sunflower.pool.TransactionPoolImpl;
import org.tdf.sunflower.service.*;
import org.tdf.sunflower.state.AccountTrie;
import org.tdf.sunflower.state.AccountUpdater;

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

    public static void loadConstants(Environment env){
        String constant = env.getProperty("sunflower.cache.trie");
        if(constant != null && !constant.isEmpty()){
            ApplicationConstants.TRIE_CACHE_SIZE = Integer.parseInt(constant);
        }
        constant = env.getProperty("sunflower.cache.p2p.transaction");
        if(constant != null && !constant.isEmpty()){
            ApplicationConstants.P2P_TRANSACTION_CACHE_SIZE = Integer.parseInt(constant);
        }
        constant = env.getProperty("sunflower.cache.p2p.proposal");
        if(constant != null && !constant.isEmpty()){
            ApplicationConstants.P2P_PROPOSAL_CACHE_SIZE = Integer.parseInt(constant);
        }
    }

    public static void loadCryptoContext(Environment env){
        String hash = env.getProperty("sunflower.crypto.hash");
        hash = (hash == null || hash.isEmpty()) ? "sm3" : hash;
        hash = hash.toLowerCase();
        switch (hash) {
            case "sm3":
                CryptoContext.hashFunction = SM3Util::hash;
                break;
            case "keccak256":
            case "keccak-256":
                CryptoContext.hashFunction = CryptoContext::keccak256;
                break;
            case "keccak512":
            case "keccak-512":
                CryptoContext.hashFunction = CryptoContext::keccak512;
                break;
            case "sha3256":
            case "sha3-256":
                CryptoContext.hashFunction = CryptoContext::sha3256;
                break;
            default:
                throw new ApplicationException("unknown hash function: " + hash);
        }
        String ec = env.getProperty("sunflower.crypto.ec");
        ec = (ec == null || ec.isEmpty()) ? "sm2" : ec;
        ec = ec.toLowerCase();
        switch (ec){
            case "ed25519":
                CryptoContext.signatureVerifier =  (pk, msg, sig) -> new Ed25519PublicKey(pk).verify(msg, sig);
                CryptoContext.signer = (sk, msg) -> new Ed25519PrivateKey(sk).sign(msg);
                CryptoContext.generateKeyPair = Ed25519::generateKeyPair;
                CryptoContext.getPkFromSk = (sk) -> new Ed25519PrivateKey(sk).generatePublicKey().getEncoded();
                break;
            case "sm2":
                CryptoContext.signatureVerifier = (pk, msg, sig) -> new SM2PublicKey(pk).verify(msg, sig);
                CryptoContext.signer = (sk, msg) -> new SM2PrivateKey(sk).sign(msg);
                CryptoContext.generateKeyPair = SM2::generateKeyPair;
                CryptoContext.getPkFromSk = (sk) -> new SM2PrivateKey(sk).generatePublicKey().getEncoded();
                break;
            default:
                throw new ApplicationException("unknown ec curve " + ec);
        }
        ApplicationConstants.PUBLIC_KEY_SIZE = CryptoContext.generateKeyPair().getPublicKey().getEncoded().length;
        log.info("use algorithm {} as hash function", hash);
        log.info("use ec {} as signature algorithm", ec);
    }

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Start.class);
        app.addInitializers(applicationContext -> {
            loadCryptoContext(applicationContext.getEnvironment());
            loadConstants(applicationContext.getEnvironment());
        });
        app.run(args);
    }

    @Bean
    public SunflowerRepository sunflowerRepository(
            ApplicationContext context, EventBus eventBus,
            DatabaseStoreFactory databaseStoreFactory
    ){
        String type = context.getEnvironment().getProperty("sunflower.database.block-store");
        type = (type == null || type.isEmpty()) ? "rdbms" : type;
        switch (type){
            case "rdbms":{
                TransactionDao transactionDao = context.getBean(TransactionDao.class);
                HeaderDao headerDao = context.getBean(HeaderDao.class);
                return new SunflowerRepositoryService(eventBus, headerDao, transactionDao);
            }
            case "kv":{
                SunflowerRepositoryKVImpl ret = new SunflowerRepositoryKVImpl(eventBus, databaseStoreFactory);
                return new ConcurrentSunflowerRepository(ret);
            }
        }
        throw new RuntimeException("unknown block store type: " + type);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return MAPPER;
    }

    @Bean
    public Miner miner(
            ConsensusEngineFacade engine,
            // this dependency asserts the peer server had been initialized
            PeerServer peerServer
    ) {
        Miner miner = engine.getMiner();
        miner.start();
        return miner;
    }

    @Bean
    public AccountTrie accountTrie(ConsensusEngineFacade consensusEngine) {
        return (AccountTrie) consensusEngine.getAccountTrie();
    }

    @Bean
    public AccountUpdater accountUpdater(AccountTrie accountTrie) {
        return (AccountUpdater) accountTrie.getUpdater();
    }

    @Bean
    public ConsensusEngineFacade consensusEngine(
            ConsensusProperties consensusProperties,
            SunflowerRepository repositoryService,
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

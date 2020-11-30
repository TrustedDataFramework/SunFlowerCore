package org.tdf.sunflower;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;
import org.tdf.common.event.EventBus;
import org.tdf.common.serialize.Codec;
import org.tdf.common.store.*;
import org.tdf.common.trie.Trie;
import org.tdf.common.util.HexBytes;
import org.tdf.crypto.CryptoHelpers;
import org.tdf.crypto.ed25519.Ed25519;
import org.tdf.crypto.ed25519.Ed25519PrivateKey;
import org.tdf.crypto.ed25519.Ed25519PublicKey;
import org.tdf.crypto.sm2.SM2;
import org.tdf.crypto.sm2.SM2PrivateKey;
import org.tdf.crypto.sm2.SM2PublicKey;
import org.tdf.gmhelper.SM2Util;
import org.tdf.gmhelper.SM3Util;
import org.tdf.sunflower.consensus.poa.PoA;
import org.tdf.sunflower.consensus.pos.PoS;
import org.tdf.sunflower.consensus.pow.PoW;
import org.tdf.sunflower.consensus.vrf.VrfEngine;
import org.tdf.sunflower.controller.WebSocket;
import org.tdf.sunflower.exception.ApplicationException;
import org.tdf.sunflower.facade.*;
import org.tdf.sunflower.net.PeerServer;
import org.tdf.sunflower.net.PeerServerImpl;
import org.tdf.sunflower.pool.TransactionPoolImpl;
import org.tdf.sunflower.service.ConcurrentSunflowerRepository;
import org.tdf.sunflower.service.HttpService;
import org.tdf.sunflower.service.SunflowerRepositoryKVImpl;
import org.tdf.sunflower.service.SunflowerRepositoryService;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.AccountTrie;
import org.tdf.sunflower.state.Address;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.CryptoContext;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.util.EnvReader;
import org.tdf.sunflower.util.FileUtils;
import org.tdf.sunflower.util.MappingUtil;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@EnableAsync
@EnableScheduling
@SpringBootApplication
@EnableTransactionManagement
@Slf4j(topic = "init")
@EnableWebSocket
// use SPRING_CONFIG_LOCATION environment to locate spring config
// for example: SPRING_CONFIG_LOCATION=classpath:\application.yml,some-path\custom-config.yml
public class Start {
    public static final ObjectMapper MAPPER = MappingUtil.OBJECT_MAPPER;
    @Getter
    private static boolean enableAssertion;
    private static ClassLoader customClassLoader = ClassUtils.getDefaultClassLoader();

    public static ClassLoader getCustomClassLoader() {
        return customClassLoader;
    }

    public static void devAssert(boolean truth, String error) {
        if (!enableAssertion) return;
        Assert.isTrue(truth, error);
    }

    public static void devAssert(Supplier<Boolean> supplier, String error) {
        if (!enableAssertion) return;
        Assert.isTrue(supplier.get(), error);
    }

    public static <T> void devAssert(T thing, Predicate<T> predicate, String error) {
        if (!enableAssertion) return;
        Assert.isTrue(predicate.test(thing), error);
    }

    @SneakyThrows
    private static Properties loadDefaultConfig() {
        Resource r = new ClassPathResource("default.config.properties");
        Properties p = new Properties();
        p.load(r.getInputStream());
        return p;
    }

    public static void loadConstants(Environment env) {
        String constant = env.getProperty("sunflower.cache.trie");
        if (constant != null && !constant.isEmpty()) {
            ApplicationConstants.TRIE_CACHE_SIZE = Integer.parseInt(constant);
        }
        constant = env.getProperty("sunflower.cache.p2p.transaction");
        if (constant != null && !constant.isEmpty()) {
            ApplicationConstants.P2P_TRANSACTION_CACHE_SIZE = Integer.parseInt(constant);
        }
        constant = env.getProperty("sunflower.cache.p2p.proposal");
        if (constant != null && !constant.isEmpty()) {
            ApplicationConstants.P2P_PROPOSAL_CACHE_SIZE = Integer.parseInt(constant);
        }
        constant = env.getProperty("sunflower.assert");
        if (constant != null && !constant.isEmpty()) {
            if (!constant.toLowerCase().matches("true|false")) throw new IllegalArgumentException("sunflower.assert");
            enableAssertion = constant.equals("true");
        }
        constant = env.getProperty("sunflower.vm.step-limit");
        if (constant != null && !constant.isEmpty())
            ApplicationConstants.VM_STEP_LIMIT = Long.parseLong(constant);

        constant = env.getProperty("sunflower.validate");
        if (constant != null && constant.trim().toLowerCase().equals("true")) {
            ApplicationConstants.VALIDATE = true;
        }
        constant = env.getProperty("sunflower.vm.max-frames");
        if (constant != null && !constant.isEmpty())
            ApplicationConstants.MAX_FRAMES = Long.parseLong(constant);

        constant = env.getProperty("sunflower.vm.max-contract-call-depth");
        if (constant != null && !constant.isEmpty())
            ApplicationConstants.MAX_CONTRACT_CALL_DEPTH = Long.parseLong(constant);

        constant = env.getProperty("sunflower.vm.gas-price");
        if (constant != null && !constant.isEmpty())
            ApplicationConstants.VM_GAS_PRICE = Long.parseLong(constant);
    }

    @SneakyThrows
    public static void loadLibs(@NonNull String path) {
        File f = new File(path);
        if (!f.exists())
            throw new RuntimeException("load libs " + path + " failed");

        List<URL> urls = new ArrayList<>();

        if (f.isDirectory()) {
            File[] files = f.listFiles();
            if (files == null)
                return;
            for (File file : files) {
                if (!file.getName().endsWith(".jar"))
                    continue;
                urls.add(file.toURI().toURL());
            }
        } else {
            urls = Collections.singletonList(f.toURI().toURL());
        }

        Start.customClassLoader = new URLClassLoader(urls.toArray(new URL[0]), ClassUtils.getDefaultClassLoader());
    }

    public static void loadCryptoContext(Environment env) {
        EnvReader reader = new EnvReader(env);
        switch (reader.getHash()) {
            case "sm3":
                CryptoContext.setHashFunction(SM3Util::hash);
                break;
            case "keccak256":
            case "keccak-256":
                CryptoContext.setHashFunction(CryptoHelpers::keccak256);
                break;
            case "keccak512":
            case "keccak-512":
                CryptoContext.setHashFunction(CryptoHelpers::keccak512);
                break;
            case "sha3256":
            case "sha3-256":
                CryptoContext.setHashFunction(CryptoHelpers::sha3256);
                break;
            default:
                throw new ApplicationException("unknown hash function: " + reader.getHash());
        }
        switch (reader.getEC()) {
            case "ed25519":
                CryptoContext.setSignatureVerifier((pk, msg, sig) -> new Ed25519PublicKey(pk).verify(msg, sig));
                CryptoContext.setSigner((sk, msg) -> new Ed25519PrivateKey(sk).sign(msg));
                CryptoContext.setSecretKeyGenerator(() -> Ed25519.generateKeyPair().getPrivateKey().getEncoded());
                CryptoContext.setGetPkFromSk((sk) -> new Ed25519PrivateKey(sk).generatePublicKey().getEncoded());
                // TODO add ed25519 ecdh
                // CryptoContext.ecdh =
                break;
            case "sm2":
                CryptoContext.setSignatureVerifier((pk, msg, sig) -> new SM2PublicKey(pk).verify(msg, sig));
                CryptoContext.setSigner((sk, msg) -> new SM2PrivateKey(sk).sign(msg));
                CryptoContext.setSecretKeyGenerator(() -> SM2.generateKeyPair().getPrivateKey().getEncoded());
                CryptoContext.setGetPkFromSk((sk) -> new SM2PrivateKey(sk).generatePublicKey().getEncoded());
                CryptoContext.setEcdh((initiator, sk, pk) -> SM2.calculateShareKey(initiator, sk, sk, pk, pk, SM2Util.WITH_ID));
                break;
            default:
                throw new ApplicationException("unknown ec curve " + reader.getEC());
        }
        CryptoContext.setPublicKeySize(CryptoContext.getPkFromSk(CryptoContext.generateSecretKey()).length);
        CryptoContext.setEncrypt(CryptoHelpers.ENCRYPT);
        CryptoContext.setDecrypt(CryptoHelpers.DECRYPT);

        log.info("use algorithm {} as hash function", reader.getHash());
        log.info("use ec {} as signature algorithm", reader.getEC());
    }

    public static void main(String[] args) {
        FileUtils.setClassLoader(ClassUtils.getDefaultClassLoader());
        SpringApplication app = new SpringApplication(Start.class);
        app.setDefaultProperties(loadDefaultConfig());
        app.addInitializers(applicationContext -> {
            loadCryptoContext(applicationContext.getEnvironment());
            loadConstants(applicationContext.getEnvironment());
            String path = applicationContext.getEnvironment().getProperty("sunflower.libs");
            if (path == null)
                return;
            loadLibs(path);
        });
        app.run(args);
    }

    @Bean
    public SunflowerRepository sunflowerRepository(
            ApplicationContext context
    ) {
        String type = context.getEnvironment().getProperty("sunflower.database.block-store");
        type = (type == null || type.isEmpty()) ? "rdbms" : type;
        switch (type) {
            case "rdbms": {
                return new SunflowerRepositoryService(context);
            }
            case "kv": {
                SunflowerRepositoryKVImpl ret = new SunflowerRepositoryKVImpl(context);
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
            ConsensusEngine engine,
            // this dependency asserts the peer server had been initialized
            PeerServer peerServer
    ) {
        Miner miner = engine.getMiner();
        miner.start();
        return miner;
    }

    @Bean
    public AccountTrie accountTrie(ConsensusEngine consensusEngine) {
        return (AccountTrie) consensusEngine.getAccountTrie();
    }

    @Bean
    public ConsensusEngine consensusEngine(
            ConsensusProperties consensusProperties,
            SunflowerRepository repositoryService,
            TransactionPoolImpl transactionPool,
            DatabaseStoreFactory databaseStoreFactory,
            EventBus eventBus,
            SyncConfig syncConfig,
            ApplicationContext context,
            @Qualifier("contractStorageTrie") Trie<byte[], byte[]> contractStorageTrie,
            @Qualifier("contractCodeStore") Store<byte[], byte[]> contractCodeStore,
            SecretStore secretStore
    ) throws Exception {
        String name = consensusProperties.getProperty(ConsensusProperties.CONSENSUS_NAME);
        name = name == null ? "" : name;
        ConsensusEngine engine;
        switch (name.trim().toLowerCase()) {
            // none consensus selected, used for unit test
            case ApplicationConstants.CONSENSUS_NONE:
                log.warn("none consensus engine selected, please ensure you are in test mode");
                engine = AbstractConsensusEngine.NONE;
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
            case ApplicationConstants.CONSENSUS_POW:
                engine = new PoW();
                break;
            case ApplicationConstants.CONSENSUS_POS:
                engine = new PoS();
                break;
            default:
                try {
                    engine = (ConsensusEngine) customClassLoader.loadClass(name).newInstance();
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error(
                            "none available consensus configured by sunflower.consensus.name=" + name +
                                    " please provide available consensus engine");
                    log.error("roll back to poa consensus");
                    engine = new PoA();
                }
        }
        injectApplicationContext(context, (AbstractConsensusEngine) engine);

        engine.init(consensusProperties);

        String[] nonNullFields = new String[]{"Miner", "Validator", "AccountTrie", "GenesisBlock", "ConfirmedBlocksProvider", "PeerServerListener"};

        for (String field : nonNullFields) {
            String method = "get" + field;
            if (engine.getClass().getMethod(method).invoke(engine) == null) {
                throw new RuntimeException("field " + field + " not injected after init");
            }
        }

        repositoryService.setProvider(engine.getConfirmedBlocksProvider());
        repositoryService.setAccountTrie(engine.getAccountTrie());

        transactionPool.setEngine(engine);
        if (engine == AbstractConsensusEngine.NONE) return engine;
        repositoryService.saveGenesis(engine.getGenesisBlock());
        if (syncConfig.getPruneHash().length > 0) {
            repositoryService.prune(syncConfig.getPruneHash());
        }

        Header best = repositoryService.getBestHeader();

        // validate trie
        if (ApplicationConstants.VALIDATE) {
            HexBytes root = best.getStateRoot();
            engine.getAccountTrie().getTrie()
                    .revert(root.getBytes())
                    .forEach((x, v) -> {
                    });
        }

        return engine;
    }

    // create peer server from properties
    @Bean
    public PeerServer peerServer(
            PeerServerProperties properties,
            ConsensusEngine engine,
            DatabaseStoreFactory factory,
            SecretStore secretStore
    ) throws Exception {
        String name = properties.getProperty("name");
        name = name == null ? "" : name;
        if (name.trim().toLowerCase().equals("none")) {
            return PeerServer.NONE;
        }
        String persist = properties.getProperty("persist");
        persist = (persist == null) ? "" : persist.trim().toLowerCase();

        Store<String, String> store = "true".equals(persist) && !"memory".equals(factory.getName()) ? new StoreWrapper<>(
                new JsonStore(Paths.get(factory.getDirectory(), "peers.json").toString(), MAPPER),
                Codec.identity(),
                new Codec<String, JsonNode>() {
                    @Override
                    public Function<? super String, ? extends JsonNode> getEncoder() {
                        return TextNode::new;
                    }

                    @Override
                    public Function<? super JsonNode, ? extends String> getDecoder() {
                        return JsonNode::asText;
                    }
                }
        ) : new MapStore<>();
        PeerServer peerServer = new PeerServerImpl(store, engine, SecretStore.NONE);
        peerServer.init(properties);
        peerServer.addListeners(engine.getPeerServerListener());
        peerServer.start();
        return peerServer;
    }


    @Bean
    public EventBus eventBus() {
        return new EventBus();
    }

    // storage root of contract store
    @Bean
    public Trie<byte[], byte[]> contractStorageTrie(DatabaseStoreFactory factory) {
        return Trie.<byte[], byte[]>builder()
                .hashFunction(CryptoContext::hash)
                .keyCodec(Codec.identity())
                .valueCodec(Codec.identity())
                .store(new NoDeleteBatchStore<>(factory.create("contract-storage-trie")))
                .build();
    }

    // contract hash code -> contract binary
    @Bean
    public Store<byte[], byte[]> contractCodeStore(DatabaseStoreFactory factory) {
        return factory.create("contract-code");
    }

    @Bean
    public SecretStore secretStore(GlobalConfig config, HttpService httpService) throws Exception {
        String ksLocation = (String) config.get("secret-store");
        if (ksLocation == null || ksLocation.isEmpty())
            return SecretStore.NONE;

        SM2PrivateKey sk = new SM2PrivateKey(SM2.generateKeyPair().getPrivateKey().getEncoded());
        SM2PublicKey pk = sk.generatePublicKey();
        log.info("please generate secret store for your private key, public key = " + HexBytes.fromBytes(pk.getEncoded()));
        log.info("waiting for load secret store...");

        while (true) {
            Map<String, String> query = new HashMap<>();
            query.put("publicKey", HexBytes.encode(pk.getEncoded()));

            String resp = null;
            try {
                resp = httpService.get(
                        HttpHeaders.EMPTY,
                        ksLocation,
                        query
                );

                SecretStoreImpl secretStore = MAPPER.readValue(
                        resp,
                        SecretStoreImpl.class);
                byte[] plain = sk.decrypt(secretStore.getCipherText().getBytes());
                if (plain.length == sk.getEncoded().length) {
                    HexBytes address = Address.fromPublicKey(CryptoContext.getPkFromSk(plain));
                    log.info("load secret store success your address = " + address);
                    return () -> HexBytes.fromBytes(plain);
                }
            } catch (Exception ignored) {

            }
            if (resp == null) {
                TimeUnit.SECONDS.sleep(1);
                continue;
            }

            log.error("invalid response from {}", ksLocation);
            resp = null;
            TimeUnit.SECONDS.sleep(1);
        }

    }

    private void injectApplicationContext(ApplicationContext context, AbstractConsensusEngine engine) {
        engine.setEventBus(context.getBean(EventBus.class));
        engine.setTransactionPool(context.getBean(TransactionPool.class));
        DatabaseStoreFactory databaseStoreFactory = (context.getBean(DatabaseStoreFactory.class));
        engine.setSunflowerRepository(context.getBean(SunflowerRepository.class));
        engine.setContractStorageTrie(context.getBean("contractStorageTrie", Trie.class));
        engine.setContractCodeStore(context.getBean("contractCodeStore", Store.class));
        engine.setSecretStore(context.getBean(SecretStore.class));
        engine.setStateTrieProvider(e -> {

            Boolean bool = context.getEnvironment().getProperty("sunflower.trie.secure", Boolean.class);

            AccountTrie trie = new AccountTrie(
                    databaseStoreFactory.create("account-trie"),
                    e.getContractCodeStore(),
                    e.getContractStorageTrie(),
                    e.getGenesisStates().stream().collect(Collectors.toMap(Account::getAddress, Function.identity())),
                    e.getPreBuiltContracts(), e.getBios(),
                    bool != null && bool
            );

            e.setAccountTrie(trie);
            Block b = e.getGenesisBlock();
            b.setStateRoot(trie.getGenesisRoot());
            e.setGenesisBlock(b);
            return trie;
        });
    }

    @Bean
    public ServerEndpointExporter serverEndpointExporter(ApplicationContext ctx) {
        WebSocket.ctx = ctx;
        return new ServerEndpointExporter();
    }
}
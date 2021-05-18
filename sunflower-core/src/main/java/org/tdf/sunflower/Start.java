package org.tdf.sunflower;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.jsonrpc4j.spring.JsonServiceExporter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.ClassUtils;
import org.tdf.common.event.EventBus;
import org.tdf.common.serialize.Codecs;
import org.tdf.common.store.*;
import org.tdf.common.trie.Trie;
import org.tdf.common.types.Uint256;
import org.tdf.common.util.HashUtil;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.consensus.poa.PoA;
import org.tdf.sunflower.consensus.pos.PoS;
import org.tdf.sunflower.consensus.pow.PoW;
import org.tdf.sunflower.controller.JsonRpc;
import org.tdf.sunflower.controller.JsonRpcFilter;
import org.tdf.sunflower.facade.*;
import org.tdf.sunflower.net.PeerServer;
import org.tdf.sunflower.net.PeerServerImpl;
import org.tdf.sunflower.pool.TransactionPoolImpl;
import org.tdf.sunflower.service.RepositoryKVImpl;
import org.tdf.sunflower.state.AccountTrie;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.ConsensusConfig;
import org.tdf.sunflower.util.FileUtils;
import org.tdf.sunflower.util.MapperUtil;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.*;


@SpringBootApplication
@Slf4j(topic = "init")
// use SPRING_CONFIG_LOCATION environment to locate spring config
// for example: SPRING_CONFIG_LOCATION=classpath:\application.yml,some-path\custom-config.yml
public class Start {
    public static final ObjectMapper MAPPER = MapperUtil.OBJECT_MAPPER;

    private static ClassLoader customClassLoader = ClassUtils.getDefaultClassLoader();

    public static ClassLoader getCustomClassLoader() {
        return customClassLoader;
    }


    @SneakyThrows
    private static Properties loadDefaultConfig() {
        Resource r = new ClassPathResource("default.config.properties");
        Properties p = new Properties();
        p.load(r.getInputStream());
        return p;
    }


    public static void loadConstants(Environment env) {
        AppConfig.INSTANCE = new AppConfig(env);
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


    public static void main(String[] args) {
        FileUtils.setClassLoader(ClassUtils.getDefaultClassLoader());
        SpringApplication app = new SpringApplication(Start.class);
        app.setDefaultProperties(loadDefaultConfig());
        app.addInitializers(applicationContext -> {
            loadConstants(applicationContext.getEnvironment());
            String path = applicationContext.getEnvironment().getProperty("sunflower.libs");
            if (path == null)
                return;
            loadLibs(path);
        });
        app.run(args);
    }

    @Bean
    public RepositoryServiceImpl sunflowerRepository(
        ApplicationContext context,
        AccountTrie accountTrie
    ) {
        String type = context.getEnvironment().getProperty("sunflower.database.block-store");
        type = (type == null || type.isEmpty()) ? "kv" : type;
        switch (type) {
            case "rdbms": {
                throw new UnsupportedOperationException();
            }
            case "kv": {
                RepositoryKVImpl kv = new RepositoryKVImpl(context);
                kv.setAccountTrie(accountTrie);
                return new RepositoryServiceImpl(
                    kv
                );
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
    public AppConfig appConfig() {
        return AppConfig.get();
    }

    @Bean
    public AccountTrie accountTrie(
        DatabaseStoreFactory databaseStoreFactory,
        @Qualifier("contractStorageTrie") Trie<HexBytes, HexBytes> contractStorageTrie,
        @Qualifier("contractCodeStore") Store<HexBytes, HexBytes> contractCodeStore
    ) {
        AppConfig c = AppConfig.get();
        return new AccountTrie(
            databaseStoreFactory.create('a'),
            contractCodeStore,
            contractStorageTrie,
            c.isTrieSecure()
        );
    }

    @Bean
    public ConsensusEngine consensusEngine(
        ConsensusProperties consensusProperties,
        RepositoryServiceImpl repoSrv,
        TransactionPoolImpl transactionPool,
        DatabaseStoreFactory databaseStoreFactory,
        EventBus eventBus,
        SyncConfig syncConfig,
        AccountTrie accountTrie,
        ApplicationContext context,
        @Qualifier("contractStorageTrie") Trie<HexBytes, HexBytes> contractStorageTrie,
        @Qualifier("contractCodeStore") Store<HexBytes, HexBytes> contractCodeStore
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
                throw new UnsupportedOperationException();
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

        engine.init(new ConsensusConfig(consensusProperties));

        String[] nonNullFields = new String[]{"Miner", "Validator", "AccountTrie", "GenesisBlock", "PeerServerListener"};

        for (String field : nonNullFields) {
            String method = "get" + field;
            if (engine.getClass().getMethod(method).invoke(engine) == null) {
                throw new RuntimeException("field " + field + " not injected after init");
            }
        }

        // init accountTrie and genesis block
        AbstractConsensusEngine abstractEngine = ((AbstractConsensusEngine) engine);
        HexBytes root = accountTrie.init(abstractEngine.getAlloc(), abstractEngine.getBios(), abstractEngine.getBuiltins());
        Block g = engine.getGenesisBlock();
        g.setStateRoot(root);

        try (RepositoryWriter writer = repoSrv.getWriter()) {
            writer.saveGenesis(g);
        }

        transactionPool.setEngine(engine);
        return engine;
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
            return PeerServer.NONE;
        }
        String persist = properties.getProperty("persist");
        persist = (persist == null) ? "" : persist.trim().toLowerCase();

        JsonStore store = "true".equals(persist) && !"memory".equals(factory.getName()) ?
            new JsonStore(Paths.get(factory.getDirectory(), "peers.json").toString(), MAPPER)
            : new JsonStore("$memory", MAPPER);
        PeerServer peerServer = new PeerServerImpl(store, engine, properties);
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
    public Trie<HexBytes, HexBytes> contractStorageTrie(DatabaseStoreFactory factory) {
        return Trie.<HexBytes, HexBytes>builder()
            .keyCodec(Codecs.HEX)
            .valueCodec(Codecs.HEX)
            .store(
                new NoDeleteStore<>(
                    factory.create('o'),
                    Store.IS_NULL
                )
            )
            .build();
    }

    // contract hash code -> contract binary
    @Bean
    public Store<HexBytes, HexBytes> contractCodeStore(DatabaseStoreFactory factory) {
        return new StoreWrapper<>(
            factory.create('c'),
            Codecs.HEX,
            Codecs.HEX
        );
    }


    // inject application context into consensus engine
    private void injectApplicationContext(
        ApplicationContext context,
        AbstractConsensusEngine engine
    ) {
        engine.setEventBus(context.getBean(EventBus.class));
        engine.setTransactionPool(context.getBean(TransactionPool.class));
        engine.setRepo(context.getBean(RepositoryService.class));
        engine.setContractStorageTrie(context.getBean("contractStorageTrie", Trie.class));
        engine.setContractCodeStore(context.getBean("contractCodeStore", Store.class));
        engine.setAccountTrie(context.getBean(AccountTrie.class));
    }

    @Bean(name = "/")
    public JsonServiceExporter jsonServiceExporter(JsonRpc jsonRpc) {
        JsonServiceExporter exporter = new JsonServiceExporter();
        exporter.setService(jsonRpc);
        exporter.setServiceInterface(JsonRpc.class);
        return exporter;
    }

    @Bean
    public FilterRegistrationBean<JsonRpcFilter> loggingFilter(JsonRpcFilter filter) {
        FilterRegistrationBean<JsonRpcFilter> registrationBean
            = new FilterRegistrationBean<>();
        registrationBean.setFilter(filter);
        registrationBean.addUrlPatterns("/");
        return registrationBean;
    }
}
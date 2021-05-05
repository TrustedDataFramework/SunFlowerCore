package org.tdf.sunflower.consensus.poa;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.tdf.common.util.HexBytes;
import org.tdf.common.util.RLPUtil;
import org.tdf.sunflower.consensus.AbstractMiner;
import org.tdf.sunflower.consensus.EconomicModel;
import org.tdf.sunflower.consensus.Proposer;
import org.tdf.sunflower.consensus.poa.config.Genesis;
import org.tdf.sunflower.consensus.poa.config.PoAConfig;
import org.tdf.sunflower.facade.AbstractConsensusEngine;
import org.tdf.sunflower.facade.PeerServerListener;
import org.tdf.sunflower.state.*;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.ConsensusConfig;
import org.tdf.sunflower.types.Transaction;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

// poa is a minimal non-trivial consensus engine
@Slf4j(topic = "poa")
public class PoA extends AbstractConsensusEngine {
    private ObjectMapper objectMapper = new ObjectMapper();

    public static final int GATEWAY_ID = 1339;
    EconomicModel economicModel;

    @Getter
    private PoAConfig config;

    private Genesis genesis;
    private PoAMiner poaMiner;
    private PoAValidator poAValidator;

    private Authentication authContract;
    private Authentication minerContract;
    private Authentication validatorContract;

    private List<PreBuiltContract> preBuiltContracts;

    private ScheduledExecutorService executorService;

    public PoA() {
        this.preBuiltContracts = new ArrayList<>();
        this.economicModel = new PoAEconomicModel();
    }

    public PoA(List<PreBuiltContract> preBuiltContracts) {
        this(preBuiltContracts, new PoAEconomicModel());
    }

    public PoA(List<PreBuiltContract> preBuiltContracts, @NonNull EconomicModel economicModel) {
        this();
        this.preBuiltContracts.addAll(preBuiltContracts);
        this.economicModel = economicModel;
    }


    @Override
    public List<HexBytes> getMinerAddresses() {
        return getMinerAddresses(getSunflowerRepository().getBestBlock().getStateRoot());
    }

    public List<HexBytes> getMinerAddresses(HexBytes parentStateRoot) {
        return minerContract.getNodes(parentStateRoot);
    }

    public Optional<Proposer> getProposer(Block parent, long currentEpochSeconds) {
        List<HexBytes> minerAddresses = getMinerAddresses(parent.getStateRoot());
        return AbstractMiner.getProposer(parent, currentEpochSeconds, minerAddresses, config.getBlockInterval());
    }

    @Override
    public List<Account> getGenesisStates() {
        return genesis.getAlloc();
    }

    @Override
    public List<PreBuiltContract> getPreBuiltContracts() {
        return preBuiltContracts;
    }

    public List<Transaction> farmBaseTransactions = new Vector<>();


    @Override
    public void init(ConsensusConfig config) {
        this.config = PoAConfig.from(config);
        genesis = new Genesis(config.getGenesisJson());

        setGenesisBlock(genesis.getBlock());

        // broadcast farm-base transaction periodically
        setPeerServerListener(PeerServerListener.NONE);
        // create state repository

        if (this.config.getThreadId() != 0 && this.config.getThreadId() != GATEWAY_ID) {
            int core = Runtime.getRuntime().availableProcessors();
            executorService = Executors.newScheduledThreadPool(
                    core > 1 ? core / 2 : core,
                    new ThreadFactoryBuilder().setNameFormat("poa-thread-%d").build()
            );

            executorService.scheduleAtFixedRate(() ->
                    {
                        try {
                            URL url = new URL(this.config.getGatewayNode());
                            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                            connection.setRequestProperty("accept", "application/json");
                            InputStream responseStream = connection.getInputStream();
                            JsonNode n = objectMapper.readValue(responseStream, JsonNode.class);
                            n = n.get("data");
                            for(int i = 0; i < n.size(); i ++) {
                                Transaction tx = RLPUtil.decode(
                                    HexBytes.fromHex(n.get(i).textValue()),
                                    Transaction.class
                                );
                                if (!getSunflowerRepository().containsTransaction(tx.getHash())) {
                                    Block best = getSunflowerRepository().getBestBlock();
                                    getTransactionPool().collect(tx);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }, 0, 30, TimeUnit.SECONDS
            );
        }

        this.authContract = new Authentication(
                genesis.getMiners(),
                Constants.PEER_AUTHENTICATION_ADDR
        );

        this.minerContract = new Authentication(
                genesis.getMiners(),
                Constants.POA_AUTHENTICATION_ADDR
        );



        this.validatorContract = new Authentication(
            genesis.getValidators(),
            Constants.VALIDATOR_CONTRACT_ADDR
        );

        preBuiltContracts.add(this.validatorContract);
        preBuiltContracts.add(this.authContract);
        preBuiltContracts.add(this.minerContract);

        initStateTrie();

        StateTrie<HexBytes, Account> trie = getAccountTrie();

        this.authContract.setAccountTrie(trie);
        this.authContract.setContractStorageTrie(getContractStorageTrie());
        this.minerContract.setAccountTrie(trie);
        this.minerContract.setContractStorageTrie(getContractStorageTrie());
        this.validatorContract.setAccountTrie(trie);
        this.validatorContract.setContractStorageTrie(getContractStorageTrie());

        poaMiner = new PoAMiner(getAccountTrie(), getEventBus(), this.config, this);
        poaMiner.setBlockRepository(this.getSunflowerRepository());
        poaMiner.setTransactionPool(getTransactionPool());

        setMiner(poaMiner);

        poAValidator = new PoAValidator(getAccountTrie(), this);
        setValidator(poAValidator);

        // register dummy account
    }

    @Override
    public int getChainId() {
        return config.getThreadId() == 0 ? super.getChainId() : config.getThreadId();
    }

    @Override
    public String getName() {
        return "poa";
    }


    public List<HexBytes> getValidators(HexBytes parentStateRoot) {
        return validatorContract.getNodes(parentStateRoot);
    }
}

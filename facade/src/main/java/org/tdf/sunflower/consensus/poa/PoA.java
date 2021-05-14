package org.tdf.sunflower.consensus.poa;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.tdf.common.util.HexBytes;
import org.tdf.common.util.RLPUtil;
import org.tdf.sunflower.consensus.EconomicModel;
import org.tdf.sunflower.consensus.poa.config.Genesis;
import org.tdf.sunflower.consensus.poa.config.PoAConfig;
import org.tdf.sunflower.facade.AbstractConsensusEngine;
import org.tdf.sunflower.facade.RepositoryReader;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.Authentication;
import org.tdf.sunflower.state.BuiltinContract;
import org.tdf.sunflower.state.Constants;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.ConsensusConfig;
import org.tdf.sunflower.types.Transaction;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
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

    @Getter
    private Authentication minerContract;
    @Getter
    private Authentication validatorContract;

    private List<BuiltinContract> builtins;

    private ScheduledExecutorService executorService;

    public PoA() {
        this.builtins = new ArrayList<>();
        this.economicModel = new PoAEconomicModel();
    }

    @Override
    public List<Account> getAlloc() {
        return genesis.getAlloc();
    }

    @Override
    public List<BuiltinContract> getBuiltins() {
        return builtins;
    }

    public List<Transaction> farmBaseTransactions = new Vector<>();


    @Override
    public void init(ConsensusConfig config) {
        this.config = PoAConfig.from(config);
        genesis = new Genesis(config.getGenesisJson());

        setGenesisBlock(genesis.getBlock());


        if (this.config.getThreadId() != 0 && this.config.getThreadId() != GATEWAY_ID) {
            int core = Runtime.getRuntime().availableProcessors();
            executorService = Executors.newScheduledThreadPool(
                core > 1 ? core / 2 : core,
                new ThreadFactoryBuilder().setNameFormat("poa-thread-%d").build()
            );

            executorService.scheduleAtFixedRate(() ->
                {
                    try (RepositoryReader rd = getSunflowerRepository().getReader()) {
                        URL url = new URL(this.config.getGatewayNode());
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                        connection.setRequestProperty("accept", "application/json");
                        InputStream responseStream = connection.getInputStream();
                        JsonNode n = objectMapper.readValue(responseStream, JsonNode.class);
                        n = n.get("data");
                        for (int i = 0; i < n.size(); i++) {
                            Transaction tx = RLPUtil.decode(
                                HexBytes.fromHex(n.get(i).textValue()),
                                Transaction.class
                            );
                            if (!rd.containsTransaction(tx.getHashHex())) {
                                Block best = rd.getBestBlock();
                                getTransactionPool().collect(tx);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }, 0, 30, TimeUnit.SECONDS
            );
        }

        this.minerContract = new Authentication(
            genesis.getMiners(),
            Constants.POA_AUTHENTICATION_ADDR,
            getAccountTrie(),
            getSunflowerRepository(),
            this.getConfig()
        );


        this.validatorContract = new Authentication(
            genesis.getValidators(),
            Constants.VALIDATOR_CONTRACT_ADDR,
            getAccountTrie(),
            getSunflowerRepository(),
            this.config
        );

        builtins.add(this.validatorContract);
        builtins.add(this.minerContract);

        poaMiner = new PoAMiner(this);
        poaMiner.setBlockRepository(this.getSunflowerRepository());
        poaMiner.setTransactionPool(getTransactionPool());

        setMiner(poaMiner);

        poAValidator = new PoAValidator(getAccountTrie(), this);
        setValidator(poAValidator);
    }

    @Override
    public int getChainId() {
        return config.getThreadId() == 0 ? super.getChainId() : config.getThreadId();
    }

    @Override
    public String getName() {
        return "poa";
    }
}

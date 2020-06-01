package org.tdf.sunflower.consensus.poa;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.consensus.AbstractMiner;
import org.tdf.sunflower.consensus.Proposer;
import org.tdf.sunflower.consensus.poa.config.Genesis;
import org.tdf.sunflower.exception.ConsensusEngineInitException;
import org.tdf.sunflower.facade.AbstractConsensusEngine;
import org.tdf.sunflower.facade.PeerServerListener;
import org.tdf.sunflower.state.*;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.util.FileUtils;
import org.tdf.sunflower.util.MappingUtil;

import java.util.*;
import java.util.stream.Collectors;

// poa is a minimal non-trivial consensus engine
@Slf4j(topic = "poa")
public class PoA extends AbstractConsensusEngine {
    private PoAConfig poAConfig;
    private Genesis genesis;
    private PoAMiner poaMiner;
    private PoAValidator poAValidator;
    private AccountTrie accountTrie;
    private Authentication authContract;
    private Authentication minerContract;

    public PoA() {
    }

    @Override
    public Optional<Set<HexBytes>> getApprovedNodes() {
        if (!poAConfig.isAuth())
            return Optional.empty();

        Block best = getSunflowerRepository().getBestBlock();
        return Optional.of(new HashSet<>(this.authContract.getNodes(best.getStateRoot().getBytes())));
    }

    @Override
    public List<HexBytes> getMinerAddresses() {
        return getMinerAddresses(getSunflowerRepository().getBestBlock().getStateRoot().getBytes());
    }

    public List<HexBytes> getMinerAddresses(byte[] parentStateRoot) {
        return minerContract.getNodes(parentStateRoot);
    }

    public Optional<Proposer> getProposer(Block parent, long currentEpochSeconds) {
        List<HexBytes> minerAddresses = getMinerAddresses(parent.getStateRoot().getBytes());
        return AbstractMiner.getProposer(parent, currentEpochSeconds, minerAddresses, poAConfig.getBlockInterval());
    }


    @Override
    public void init(Properties properties) throws ConsensusEngineInitException {
        ObjectMapper objectMapper = new ObjectMapper().enable(JsonParser.Feature.ALLOW_COMMENTS);
        poAConfig = MappingUtil.propertiesToPojo(properties, PoAConfig.class);
        Resource resource;
        try {
            resource = FileUtils.getResource(poAConfig.getGenesis());
        } catch (Exception e) {
            throw new ConsensusEngineInitException(e.getMessage());
        }
        try {
            genesis = objectMapper.readValue(resource.getInputStream(), Genesis.class);
        } catch (Exception e) {
            throw new ConsensusEngineInitException("failed to parse genesis");
        }

        setGenesisBlock(genesis.getBlock());

        setPeerServerListener(PeerServerListener.NONE);
        // create state repository

        Map<HexBytes, Account> alloc = new HashMap<>();

        if (genesis.alloc != null) {
            genesis.alloc.forEach((k, v) -> {
                Account a = new Account(HexBytes.fromHex(k), v);
                alloc.put(a.getAddress(), a);
            });
        }

        List<PreBuiltContract> preBuiltContracts = new ArrayList<>();

        this.authContract = poAConfig.isAuth() ? new Authentication(
                genesis.miners == null ? Collections.emptyList() :
                        genesis.miners.stream().map(Genesis.MinerInfo::getAddress).collect(Collectors.toSet()),
                Constants.PEER_AUTHENTICATION_ADDR
        ) : null;

        if (this.authContract != null)
            preBuiltContracts.add(this.authContract);

        this.minerContract = new Authentication(genesis.miners == null ? Collections.emptyList() :
                genesis.miners.stream().map(Genesis.MinerInfo::getAddress).collect(Collectors.toSet()),
                Constants.POA_AUTHENTICATION_ADDR
        );

        preBuiltContracts.add(this.minerContract);

        AccountUpdater updater = new AccountUpdater(
                alloc, getContractCodeStore(),
                getContractStorageTrie(),
                preBuiltContracts,
                Collections.emptyList()
        );


        AccountTrie trie = new AccountTrie(
                updater, getDatabaseStoreFactory(),
                getContractCodeStore(), getContractStorageTrie()
        );

        this.accountTrie = trie;
        this.authContract.setAccountTrie(trie);
        this.minerContract.setAccountTrie(trie);
        getGenesisBlock().setStateRoot(trie.getGenesisRoot());
        setAccountTrie(trie);

        poaMiner = new PoAMiner(getAccountTrie(), getEventBus(), poAConfig, this);
        poaMiner.setBlockRepository(this.getSunflowerRepository());
        poaMiner.setPoAConfig(poAConfig);
        poaMiner.setTransactionPool(getTransactionPool());

        setMiner(poaMiner);

        poAValidator = new PoAValidator(getAccountTrie(), this);
        setValidator(poAValidator);

        // register dummy account
    }

    @Override
    public String getName() {
        return "poa";
    }
}

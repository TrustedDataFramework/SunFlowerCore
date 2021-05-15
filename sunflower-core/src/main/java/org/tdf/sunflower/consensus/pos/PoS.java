package org.tdf.sunflower.consensus.pos;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.tdf.common.types.Uint256;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.facade.AbstractConsensusEngine;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.AccountTrie;
import org.tdf.sunflower.state.BuiltinContract;
import org.tdf.sunflower.types.ConsensusConfig;

import java.util.*;
import java.util.stream.Collectors;

import static org.tdf.sunflower.consensus.pos.PosPreBuilt.NodeInfo;

@Slf4j(topic = "pos")
public class PoS extends AbstractConsensusEngine {
    private ConsensusConfig config;

    private PosPreBuilt minerContract;

    private PoSValidator posValidator;

    private PoSMiner posMiner;

    private Genesis genesis;

    public PoS() {

    }

    public List<HexBytes> getMinerAddresses(HexBytes stateRoot) {
        return this.minerContract.getNodes(stateRoot).stream().limit(config.getMaxMiners()).collect(Collectors.toList());
    }

    @Override
    public Map<HexBytes, Account> getAlloc() {
        return genesis.getAlloc();
    }

    @Override
    public List<BuiltinContract> getBuiltins() {
        return Collections.singletonList(minerContract);
    }

    @Override
    @SneakyThrows
    public void init(ConsensusConfig config) {
        this.config = config;
        genesis = new Genesis(config.getGenesisJson());

        setGenesisBlock(genesis.getBlock());
        Map<HexBytes, NodeInfo> nodesMap = new TreeMap<>();
        if (genesis.miners != null) {
            genesis.miners.forEach(
                m -> nodesMap.put(m.getAddress(), new NodeInfo(m.getAddress(), Uint256.of(m.getVote()),
                    new ArrayList<>())));
        }

        this.minerContract = new PosPreBuilt(nodesMap);
        this.minerContract.setAccountTrie((AccountTrie) getAccountTrie());

        this.posMiner = new PoSMiner(getAccountTrie(), getEventBus(), this.config, this);
        this.posMiner.setRepo(this.getRepo());
        this.posMiner.setTransactionPool(getTransactionPool());

        setMiner(this.posMiner);

        this.posValidator = new PoSValidator(getAccountTrie(), this.posMiner);
        setValidator(this.posValidator);
    }

    @Override
    public String getName() {
        return "pos";
    }
}

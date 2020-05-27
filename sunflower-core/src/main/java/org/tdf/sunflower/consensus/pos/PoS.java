package org.tdf.sunflower.consensus.pos;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.tdf.common.util.BigEndian;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.consensus.poa.config.Genesis;
import org.tdf.sunflower.exception.ConsensusEngineInitException;
import org.tdf.sunflower.facade.AbstractConsensusEngine;
import org.tdf.sunflower.facade.PeerServerListener;
import org.tdf.sunflower.state.*;
import org.tdf.sunflower.util.FileUtils;
import org.tdf.sunflower.util.MappingUtil;

import java.util.*;

import static org.tdf.sunflower.consensus.pos.PosPreBuilt.NodeInfo;

@Slf4j(topic = "pos")
public class PoS extends AbstractConsensusEngine {
    public static final int BLOCK_VERSION = BigEndian.decodeInt32(new byte[]{0, 'p', 'o', 's'});
    public static final int TRANSACTION_VERSION = BigEndian.decodeInt32(new byte[]{0, 'p', 'o', 's'});

    private PosPreBuilt minerContract;

    private PoSValidator posValidator;

    private PoSMiner posMiner;

    public PoS(Properties properties) {
        super(properties);
    }

    public List<HexBytes> getMinerAddresses() {
        return getMinerAddresses(getSunflowerRepository().getBestBlock().getStateRoot().getBytes());
    }

    public List<HexBytes> getMinerAddresses(byte[] stateRoot) {
        return this.minerContract.getNodes(stateRoot);
    }

    @Override
    public Optional<Set<HexBytes>> getApprovedNodes() {
        return Optional.empty();
    }

    @Override
    @SneakyThrows
    public void init(Properties properties) throws ConsensusEngineInitException {
        ObjectMapper objectMapper = new ObjectMapper().enable(JsonParser.Feature.ALLOW_COMMENTS);
        PoSConfig posConfig = MappingUtil.propertiesToPojo(properties, PoSConfig.class);
        Resource resource = FileUtils.getResource(posConfig.getGenesis());
        Genesis genesis = objectMapper.readValue(resource.getInputStream(), Genesis.class);

        Map<HexBytes, Account> map = new HashMap<>();
        if (genesis.alloc != null) {
            genesis.alloc.forEach((k, v) -> {
                map.put(HexBytes.fromHex(k), new Account(HexBytes.fromHex(k), v));
            });
        }

        List<PreBuiltContract> preBuiltContracts = new ArrayList<>();
        Map<HexBytes, NodeInfo> nodesMap = new TreeMap<>();
        if (genesis.miners != null) {
            genesis.miners.forEach(m -> {
                nodesMap.put(m.getAddress(), new NodeInfo(m.getAddress(), 0L));
            });
        }
        this.minerContract = new PosPreBuilt(nodesMap);
        preBuiltContracts.add(this.minerContract);
        AccountUpdater updater = new AccountUpdater(
                map, getContractCodeStore(), getContractStorageTrie(),
                preBuiltContracts, Collections.emptyList()
        );
        AccountTrie trie = new AccountTrie(
                updater, getDatabaseStoreFactory(),
                getContractCodeStore(), getContractStorageTrie()
        );

        this.minerContract.setAccountTrie(trie);
        setGenesisBlock(genesis.getBlock());
        setPeerServerListener(PeerServerListener.NONE);
        setAccountTrie(trie);

        this.posMiner = new PoSMiner(getAccountTrie(), getEventBus(), posConfig, this);
        this.posMiner.setBlockRepository(this.getSunflowerRepository());
        this.posMiner.setTransactionPool(getTransactionPool());
        this.posMiner.setPoSConfig(posConfig);
        setMiner(this.posMiner);

        this.posValidator = new PoSValidator(getAccountTrie(), this.posMiner);
        setValidator(this.posValidator);
    }
}

package org.tdf.sunflower.consensus.pos;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.tdf.common.types.Uint256;
import org.tdf.common.util.BigEndian;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.exception.ConsensusEngineInitException;
import org.tdf.sunflower.facade.AbstractConsensusEngine;
import org.tdf.sunflower.facade.PeerServerListener;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.AccountTrie;
import org.tdf.sunflower.state.Constants;
import org.tdf.sunflower.state.PreBuiltContract;
import org.tdf.sunflower.util.FileUtils;
import org.tdf.sunflower.util.MappingUtil;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import static org.tdf.sunflower.consensus.pos.PosPreBuilt.NodeInfo;

@Slf4j(topic = "pos")
public class PoS extends AbstractConsensusEngine {
    public static final int BLOCK_VERSION = BigEndian.decodeInt32(new byte[]{0, 'p', 'o', 's'});
    public static final int TRANSACTION_VERSION = BigEndian.decodeInt32(new byte[]{0, 'p', 'o', 's'});

    private PoSConfig posConfig;

    private PosPreBuilt minerContract;

    private PoSValidator posValidator;

    private PoSMiner posMiner;

    private Genesis genesis;

    public PoS() {

    }

    public List<HexBytes> getMinerAddresses() {
        return getMinerAddresses(getSunflowerRepository().getBestBlock().getStateRoot());
    }

    public List<HexBytes> getMinerAddresses(HexBytes stateRoot) {
        return this.minerContract.getNodes(stateRoot).stream().limit(posConfig.getMaxMiners()).collect(Collectors.toList());
    }

    @Override
    public Optional<Set<HexBytes>> getApprovedNodes() {
        return Optional.empty();
    }

    @Override
    public List<Account> getGenesisStates() {
        return genesis.alloc == null ? Collections.emptyList() :
                genesis.alloc.entrySet().stream()
                        .map(e -> Account.emptyAccount(HexBytes.fromHex(e.getKey()), Uint256.of(e.getValue())))
                        .collect(Collectors.toList());
    }

    @Override
    public List<PreBuiltContract> getPreBuiltContracts() {
        return Collections.singletonList(minerContract);
    }

    @Override
    @SneakyThrows
    public void init(Properties properties) throws ConsensusEngineInitException {
        ObjectMapper objectMapper = new ObjectMapper().enable(JsonParser.Feature.ALLOW_COMMENTS);
        posConfig = MappingUtil.propertiesToPojo(properties, PoSConfig.class);
        InputStream in = FileUtils.getInputStream(posConfig.getGenesis());
        genesis = objectMapper.readValue(in, Genesis.class);
        setGenesisBlock(genesis.getBlock());

        Map<HexBytes, NodeInfo> nodesMap = new TreeMap<>();
        if (genesis.miners != null) {
            genesis.miners.forEach(
                    m -> nodesMap.put(m.getAddress(), new NodeInfo(m.getAddress(), Uint256.of(m.vote),
                            new ArrayList<>())));
        }

        this.minerContract = new PosPreBuilt(nodesMap);
        initStateTrie();
        this.minerContract.setAccountTrie((AccountTrie) getAccountTrie());
        setPeerServerListener(PeerServerListener.NONE);

        this.posMiner = new PoSMiner(getAccountTrie(), getEventBus(), posConfig, this);
        this.posMiner.setBlockRepository(this.getSunflowerRepository());
        this.posMiner.setTransactionPool(getTransactionPool());
        this.posMiner.setPoSConfig(posConfig);
        setMiner(this.posMiner);

        this.posValidator = new PoSValidator(getAccountTrie(), this.posMiner);
        setValidator(this.posValidator);
    }

    @Override
    public String getName() {
        return "pos";
    }
}

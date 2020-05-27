package org.tdf.sunflower.consensus.pos;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.tdf.common.util.BigEndian;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.exception.ConsensusEngineInitException;
import org.tdf.sunflower.facade.AbstractConsensusEngine;
import org.tdf.sunflower.facade.PeerServerListener;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.AccountTrie;
import org.tdf.sunflower.util.FileUtils;
import org.tdf.sunflower.util.MappingUtil;

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

    public PoS(Properties properties) {
        super(properties);
    }

    public List<HexBytes> getMinerAddresses() {
        return getMinerAddresses(getSunflowerRepository().getBestBlock().getStateRoot().getBytes());
    }

    public List<HexBytes> getMinerAddresses(byte[] stateRoot) {
        return this.minerContract.getNodes(stateRoot).stream().limit(posConfig.getMaxMiners()).collect(Collectors.toList());
    }

    @Override
    public Optional<Set<HexBytes>> getApprovedNodes() {
        return Optional.empty();
    }

    @Override
    @SneakyThrows
    public void init(Properties properties) throws ConsensusEngineInitException {
        ObjectMapper objectMapper = new ObjectMapper().enable(JsonParser.Feature.ALLOW_COMMENTS);
        posConfig = MappingUtil.propertiesToPojo(properties, PoSConfig.class);
        Resource resource = FileUtils.getResource(posConfig.getGenesis());
        Genesis genesis = objectMapper.readValue(resource.getInputStream(), Genesis.class);

        List<Account> alloc =
                genesis.alloc.entrySet().stream()
                        .map(e -> new Account(HexBytes.fromHex(e.getKey()), e.getValue())).collect(Collectors.toList());

        Map<HexBytes, NodeInfo> nodesMap = new TreeMap<>();
        if (genesis.miners != null) {
            genesis.miners.forEach(m -> nodesMap.put(m.getAddress(), new NodeInfo(m.getAddress(), m.vote)));
        }
        this.minerContract = new PosPreBuilt(nodesMap);
        initStates(genesis.getBlock(), alloc, Collections.singletonList(this.minerContract), Collections.emptyList());
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
}

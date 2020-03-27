package org.tdf.sunflower.consensus.vrf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.springframework.core.io.Resource;
import org.tdf.common.util.HexBytes;
import org.tdf.rlp.RLPCodec;
import org.tdf.rlp.RLPElement;
import org.tdf.sunflower.consensus.vrf.VrfGenesis.MinerInfo;
import org.tdf.sunflower.consensus.vrf.contract.VrfBiosContractUpdater;
import org.tdf.sunflower.consensus.vrf.core.CommitProof;
import org.tdf.sunflower.consensus.vrf.core.PendingVrfState;
import org.tdf.sunflower.consensus.vrf.core.ProposalProof;
import org.tdf.sunflower.consensus.vrf.core.ValidatorManager;
import org.tdf.sunflower.consensus.vrf.core.VrfBlockWrapper;
import org.tdf.sunflower.consensus.vrf.util.VrfMessageCode;
import org.tdf.sunflower.consensus.vrf.util.VrfUtil;
import org.tdf.sunflower.consensus.vrf.util.VrfUtil.VrfMessageCodeAndBytes;
import org.tdf.sunflower.events.NewBlockMined;
import org.tdf.sunflower.exception.ConsensusEngineInitException;
import org.tdf.sunflower.facade.ConsensusEngine;
import org.tdf.sunflower.facade.PeerServerListener;
import org.tdf.sunflower.facade.SunflowerRepository;
import org.tdf.sunflower.net.Context;
import org.tdf.sunflower.net.MessageBuilder;
import org.tdf.sunflower.net.Peer;
import org.tdf.sunflower.net.PeerImpl;
import org.tdf.sunflower.net.PeerServer;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.AccountTrie;
import org.tdf.sunflower.state.AccountUpdater;
import org.tdf.sunflower.state.BiosContractUpdater;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.util.ByteUtil;
import org.tdf.sunflower.util.FileUtils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@Slf4j
public class VrfEngine extends ConsensusEngine implements PeerServerListener {
    private VrfGenesis genesis;
    private VrfConfig vrfConfig;
    private VrfStateMachine vrfStateMachine;
    private MessageBuilder messageBuilder;

    private PeerServer peerServer;
    private VrfMiner vrfMiner;
//    private ConsortiumRepository blockRepository;

    public VrfEngine() {

    }

    @Override
    public void onMessage(Context context, PeerServer server) {
        byte[] messageBytes = context.getMessage();
        if (RLPElement.fromEncoded(messageBytes).asRLPList().get(0).asInt() > VrfMessageCode.NEW_MINED_BLOCK
                .ordinal()) {
            return;
        }
        VrfMessageCodeAndBytes codeAndBytes = VrfUtil.parseMessageBytes(messageBytes);
        VrfMessageCode code = codeAndBytes.getCode();
        byte[] vrfBytes = codeAndBytes.getRlpBytes();
        switch (code) {
        case VRF_BLOCK:
            processVrfBlockMsg(context, vrfBytes);
            break;
        case VRF_PROPOSAL_PROOF:
            processVrfProposalProofMsg(vrfBytes);
            break;
        case VRF_COMMIT_PROOF:
            processCommitProofMsg(vrfBytes);
            break;
        case NEW_MINED_BLOCK:
            processNewMinedBlockMsg(vrfBytes);
            break;
        }
    }

    private void processNewMinedBlockMsg(byte[] bodyBytes) {
        Block blockNew = RLPCodec.decode(bodyBytes, Block.class);
        log.info("New mined block received from peer. Num #{}, Hash {}", blockNew.getHeight(),
                blockNew.getHash().toString());
        // merged into org.tdf.sunflower.service.NewMinedBlockWriter
        // saveBlock(blockNew, this.getSunflowerRepository(), this);
        vrfMiner.stop();
        vrfMiner.start();
    }

    private void processCommitProofMsg(byte[] bodyBytes) {
        CommitProof commitProof = RLPCodec.decode(bodyBytes, CommitProof.class);
        log.info("VRF CommitProof received. Round #{}, miner {}, block hash {}", commitProof.getRound(),
                ByteUtil.toHexString(commitProof.getCoinbase()),
                ByteUtil.toHexString(commitProof.getBlockIdentifier().getHash()));
        vrfStateMachine.addProof(commitProof, true);
    }

    private void processVrfProposalProofMsg(byte[] bodyBytes) {
        ProposalProof proposalProof = RLPCodec.decode(bodyBytes, ProposalProof.class);
        log.info("VRF ProposalProof received. Round #{}, miner {}, block hash {}", proposalProof.getRound(),
                ByteUtil.toHexString(proposalProof.getCoinbase()),
                ByteUtil.toHexString(proposalProof.getBlockIdentifier().getHash()));
        vrfStateMachine.addProof(proposalProof, true);
    }

    private void processVrfBlockMsg(Context context, byte[] bodyBytes) {
        Block block = RLPCodec.decode(bodyBytes, Block.class);
        log.info("VRF block received. Num #{}, Hash {}", block.getHeight(), block.getHash().toString());
        vrfStateMachine.addNewBlock(new VrfBlockWrapper(block, context.getRemote().getID().getBytes()));
    }

    @Override
    public void onStart(PeerServer server) {

        setPeerServer(server);
        setMessageBuilder(new MessageBuilder((PeerImpl) peerServer.getSelf()));
        vrfMiner.setPeerServer(peerServer);
        vrfMiner.setMessageBuilder(messageBuilder);

        getEventBus().subscribe(NewBlockMined.class, (e) -> {
            Block block = e.getBlock();
            log.info("!!! Wow, new block mined. #{}, {}", block.getHeight(),
                    ByteUtil.toHexString(block.getHash().getBytes()));
//                byte[] encoded = RLPSerializer.SERIALIZER.serialize(block);
//                Message message = messageBuilder.buildMessage(Code.NEW_MINED_BLOCK, VrfConstants.MESSAGE_TTL, encoded);
            byte[] encoded = VrfUtil.buildMessageBytes(VrfMessageCode.NEW_MINED_BLOCK, block);
            peerServer.broadcast(encoded);
        });
    }

    @Override
    public void onNewPeer(Peer peer, PeerServer server) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onDisconnect(Peer peer, PeerServer server) {
        // TODO Auto-generated method stub

    }

    @Override
    public void init(Properties properties) throws ConsensusEngineInitException {
        JavaPropsMapper mapper = new JavaPropsMapper();
        ObjectMapper objectMapper = new ObjectMapper().enable(JsonParser.Feature.ALLOW_COMMENTS);
        try {
            vrfConfig = mapper.readPropertiesAs(properties, VrfConfig.class);
        } catch (Exception e) {
            String schema = "";
            try {
                schema = mapper.writeValueAsProperties(new VrfConfig()).toString();
            } catch (Exception ignored) {
            }
            throw new ConsensusEngineInitException(
                    "load properties failed :" + properties.toString() + " expecting " + schema);
        }
        vrfMiner = new VrfMiner(vrfConfig);
        Resource resource;
        try {
            resource = FileUtils.getResource(vrfConfig.getGenesis());
        } catch (Exception e) {
            throw new ConsensusEngineInitException(e.getMessage());
        }
        try {
            genesis = objectMapper.readValue(resource.getInputStream(), VrfGenesis.class);
        } catch (Exception e) {
            throw new ConsensusEngineInitException("failed to parse genesis: " + e.getMessage());
        }

        vrfMiner.setBlockRepository(this.getSunflowerRepository());
        vrfMiner.setConfig(vrfConfig);
        vrfMiner.setGenesis(genesis);
        vrfMiner.setTransactionPool(getTransactionPool());
        setMiner(vrfMiner);
        try {
            setGenesisBlock(genesis.getBlock(vrfConfig));
        } catch (IOException e) {
            throw new ConsensusEngineInitException(e);
        }

        setPeerServerListener(this);

        Map<HexBytes, Account> alloc = new HashMap<>();
        if (genesis.alloc != null) {
            genesis.alloc.forEach((k, v) -> {
                Account a = new Account(HexBytes.fromHex(k), v);
                alloc.put(a.getAddress(), a);
            });
        }

        // Precompiled contracts
        VrfBiosContractUpdater vrfContract = new VrfBiosContractUpdater();
        // Set collaterals from genesis
        setGenesisCollateral(genesis.miners, vrfContract.getGenesisStorage());
        List<BiosContractUpdater> contractList = new ArrayList<>();
        contractList.add(vrfContract);

        AccountUpdater updater = new AccountUpdater(alloc, getContractCodeStore(), getContractStorageTrie(),
                contractList);
        AccountTrie trie = new AccountTrie(updater, getDatabaseStoreFactory(), getContractCodeStore(),
                getContractStorageTrie());
        getGenesisBlock().setStateRoot(trie.getGenesisRoot());
        setAccountTrie(trie);
        setValidator(new VrfValidator(getAccountTrie()));
        vrfMiner.setAccountTrie(getAccountTrie());
        vrfMiner.setEventBus(getEventBus());

//        setConfirmedBlocksProvider(unconfirmed -> unconfirmed);
        // ------- Need well implementation.
        ValidatorManager validatorManager = new ValidatorManager(this.getSunflowerRepository());

        vrfStateMachine = new VrfStateMachine(validatorManager, new PendingVrfState(validatorManager),
                new VrfValidator(getAccountTrie()));
        vrfMiner.setVrfStateMachine(vrfStateMachine);

        // register miner accounts
//        getStateRepository().register(getGenesisBlock(),
//                Collections.singleton(new Account(vrfMiner.minerPublicKeyHash, 0)));

    }

    private void setGenesisCollateral(List<MinerInfo> miners, Map<byte[], byte[]> storage) {
        miners.forEach(miner -> {
            storage.put(miner.address.getBytes(), ByteUtil.longToBytes(miner.collateral));
        });
    }

    @Override
    public Block getGenesisBlock() {
        Block genesisBlock = super.getGenesisBlock();
        if (genesisBlock != null)
            return genesisBlock;
        try {
            genesisBlock = genesis.getBlock(vrfConfig);
        } catch (IOException e) {
            e.printStackTrace();
        }
        setGenesisBlock(genesisBlock);
        return genesisBlock;
    }

    private boolean saveBlock(Block block, SunflowerRepository repository, ConsensusEngine engine) {
        Optional<Block> o = repository.getBlock(block.getHashPrev().getBytes());
        if (!o.isPresent())
            return false;
        if (engine.getValidator().validate(block, o.get()).isSuccess()) {
            repository.writeBlock(block);
        }
        return true;
    }
}

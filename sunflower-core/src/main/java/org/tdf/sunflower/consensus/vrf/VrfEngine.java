package org.tdf.sunflower.consensus.vrf;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.tdf.common.util.HexBytes;
import org.tdf.rlp.RLPCodec;
import org.tdf.rlp.RLPElement;
import org.tdf.sunflower.consensus.vrf.VrfGenesis.MinerInfo;
import org.tdf.sunflower.consensus.vrf.contract.VrfPreBuiltContract;
import org.tdf.sunflower.consensus.vrf.core.*;
import org.tdf.sunflower.consensus.vrf.util.VrfMessageCode;
import org.tdf.sunflower.consensus.vrf.util.VrfUtil;
import org.tdf.sunflower.consensus.vrf.util.VrfUtil.VrfMessageCodeAndBytes;
import org.tdf.sunflower.events.NewBlockMined;
import org.tdf.sunflower.exception.ConsensusEngineInitException;
import org.tdf.sunflower.facade.AbstractConsensusEngine;
import org.tdf.sunflower.facade.PeerServerListener;
import org.tdf.sunflower.facade.SunflowerRepository;
import org.tdf.sunflower.net.Context;
import org.tdf.sunflower.net.Peer;
import org.tdf.sunflower.net.PeerServer;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.AccountTrie;
import org.tdf.sunflower.state.PreBuiltContract;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.util.ByteUtil;
import org.tdf.sunflower.util.FileUtils;
import org.tdf.sunflower.util.MappingUtil;

import java.io.IOException;
import java.util.*;

@Getter
@Setter
@Slf4j
public class VrfEngine extends AbstractConsensusEngine implements PeerServerListener {
    private VrfGenesis genesis;
    private VrfConfig vrfConfig;
    private VrfStateMachine vrfStateMachine;

    private PeerServer peerServer;
    private VrfMiner vrfMiner;
    private List<PreBuiltContract> contractList = new ArrayList<>();

    @Override
    public List<Account> getGenesisStates() {
        List<Account> ret = new ArrayList<>();
        if (genesis.alloc != null) {
            genesis.alloc.forEach((k, v) -> {
                Account a = new Account(HexBytes.fromHex(k), v);
                ret.add(a);
            });
        }
        return ret;
    }

    @Override
    public List<PreBuiltContract> getPreBuiltContracts() {
        return contractList;
    }


    public VrfEngine() {

    }
//    private ConsortiumRepository blockRepository;


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
        vrfMiner.setPeerServer(peerServer);

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
        ObjectMapper objectMapper = new ObjectMapper().enable(JsonParser.Feature.ALLOW_COMMENTS);
        vrfConfig = MappingUtil.propertiesToPojo(properties, VrfConfig.class);
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

        try {
            setGenesisBlock(genesis.getBlock(vrfConfig));
        } catch (IOException e) {
            throw new ConsensusEngineInitException(e);
        }

        setPeerServerListener(this);


        // Precompiled contracts
        VrfPreBuiltContract vrfContract = new VrfPreBuiltContract();

        // Set collaterals from genesis
        setGenesisCollateral(genesis.miners, vrfContract);
        contractList.add(vrfContract);

        initStateTrie();

        setValidator(new VrfValidator(getAccountTrie()));

        vrfMiner = new VrfMiner(getAccountTrie(), getEventBus(), vrfConfig);
        vrfMiner.setBlockRepository(this.getSunflowerRepository());
        vrfMiner.setConfig(vrfConfig);
        vrfMiner.setGenesis(genesis);
        vrfMiner.setTransactionPool(getTransactionPool());
        setMiner(vrfMiner);
        vrfMiner.setContractStorageTrie(getContractStorageTrie());

//        setConfirmedBlocksProvider(unconfirmed -> unconfirmed);
        // ------- Need well implementation.
        AccountTrie trie = (AccountTrie) getAccountTrie();
        ValidatorManager validatorManager = new ValidatorManager(this.getSunflowerRepository(), trie,
                getContractStorageTrie(), vrfConfig);

        vrfStateMachine = new VrfStateMachine(validatorManager, new PendingVrfState(validatorManager),
                new VrfValidator(getAccountTrie()));
        vrfMiner.setVrfStateMachine(vrfStateMachine);

        // register miner accounts
//        getStateRepository().register(getGenesisBlock(),
//                Collections.singleton(new Account(vrfMiner.minerPublicKeyHash, 0)));

    }

    private void setGenesisCollateral(List<MinerInfo> miners, VrfPreBuiltContract vrfBiosContractUpdater) {
        long total = 0;
        Map<byte[], byte[]> storage = vrfBiosContractUpdater.getGenesisStorage();
        Account contractAccount = vrfBiosContractUpdater.getGenesisAccount();
        for (MinerInfo miner : miners) {
            storage.put(miner.address.getBytes(), ByteUtil.longToBytes(miner.collateral));
            total += miner.collateral;
            contractAccount.setBalance(contractAccount.getBalance() + miner.collateral);
        }
        storage.put(VrfPreBuiltContract.TOTAL_KEY, ByteUtil.longToBytes(total));
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

    private boolean saveBlock(Block block, SunflowerRepository repository, AbstractConsensusEngine engine) {
        Optional<Block> o = repository.getBlock(block.getHashPrev().getBytes());
        if (!o.isPresent())
            return false;
        if (engine.getValidator().validate(block, o.get()).isSuccess()) {
            repository.writeBlock(block);
        }
        return true;
    }

    @Override
    public String getName() {
        return "vrf";
    }
}

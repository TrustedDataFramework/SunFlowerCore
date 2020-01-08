package org.tdf.sunflower.consensus.vrf.util;

import java.io.File;
import java.io.IOException;

import org.tdf.common.util.HexBytes;
import org.tdf.crypto.PrivateKey;
import org.tdf.crypto.ed25519.Ed25519;
import org.tdf.crypto.ed25519.Ed25519PrivateKey;
import org.tdf.rlp.RLPCodec;
import org.tdf.rlp.RLPElement;
import org.tdf.rlp.RLPItem;
import org.tdf.rlp.RLPList;
import org.tdf.sunflower.consensus.vrf.VrfConfig;
import org.tdf.sunflower.consensus.vrf.core.BlockIdentifier;
import org.tdf.sunflower.consensus.vrf.core.CommitProof;
import org.tdf.sunflower.consensus.vrf.core.ProposalProof;
import org.tdf.sunflower.consensus.vrf.core.VrfBlockWrapper;
import org.tdf.sunflower.consensus.vrf.core.VrfProof;
import org.tdf.sunflower.consensus.vrf.keystore.FileSystemKeystore;
import org.tdf.sunflower.consensus.vrf.struct.VrfBlockFields;
import org.tdf.sunflower.consensus.vrf.struct.VrfPrivateKey;
import org.tdf.sunflower.consensus.vrf.struct.VrfResult;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.util.ByteUtil;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VrfUtil {
    public static String VRF_PK = "c42e9a44063bfd0956da144d6500ca05351507f55f2490b3966c78a4d7e096ca";
    public static final String VRF_SK = "065994b6ccef45a1dcabbbd77cc11638308142b8c50e08845c3f1e0eeefa8dee";

    // for testing purposes when the timer might be changed
    // to manage current time according to test scenarios
    public static Timer TIMER = new Timer();

    public static class Timer {
        public long curTime() {
            return System.currentTimeMillis();
        }
    }

    public static long curTime() {
        return TIMER.curTime();
    }

    public static byte[] getMiner(HexBytes payload) {
        byte[] encoded = payload.getBytes();
        VrfBlockFields vrfBlockFields = RLPCodec.decode(encoded, VrfBlockFields.class);
        return vrfBlockFields.getMiner();
    }

    public static byte[] getMiner(Header header) {
        return getMiner(header.getPayload());
    }

    public static byte[] getMiner(Block block) {
        return getMiner(block.getPayload());
    }

    public static ProposalProof getProposalProof(HexBytes payload) {
        byte[] encoded = payload.getBytes();
        VrfBlockFields vrfBlockFields = RLPCodec.decode(encoded, VrfBlockFields.class);
        return RLPCodec.decode(vrfBlockFields.getProposalProof(), ProposalProof.class);
    }

    public static ProposalProof getProposalProof(Header header) {
        return getProposalProof(header.getPayload());
    }

    public static ProposalProof getProposalProof(Block block) {
        return getProposalProof(block.getPayload());
    }

    public static byte[] getSeed(HexBytes payload) {
        byte[] encoded = payload.getBytes();
        VrfBlockFields vrfBlockFields = RLPCodec.decode(encoded, VrfBlockFields.class);
        return vrfBlockFields.getSeed();
    }

    public static byte[] getSeed(Header header) {
        return getSeed(header.getPayload());
    }

    public static byte[] getSeed(Block block) {
        return getSeed(block.getPayload());
    }

    public static byte[] getSeed(VrfBlockWrapper blockWrapper) {
        return getSeed(blockWrapper.getBlock());
    }

    public static byte[] getPriority(HexBytes payload) {
        byte[] encoded = payload.getBytes();
        VrfBlockFields vrfBlockFields = RLPCodec.decode(encoded, VrfBlockFields.class);
        return vrfBlockFields.getPriority();
    }

    public static byte[] getPriority(Header header) {
        return getPriority(header.getPayload());
    }

    public static byte[] getPriority(Block block) {
        return getPriority(block.getPayload());
    }

    public static void setSeed(Header header, byte[] seed) {
        HexBytes payload = header.getPayload();
        VrfBlockFields vrfBlockFields = getVrfBlockFields(payload);
        vrfBlockFields.setSeed(seed);

        byte[] encoded = RLPCodec.encode(vrfBlockFields);
        header.setPayload(HexBytes.fromBytes(encoded));
    }

    public static void setSeed(Block block, byte[] seed) {
        setSeed(block.getHeader(), seed);
    }

    public static void setSeed(Block block, HexBytes seed) {
        setSeed(block, seed.getBytes());
    }

    public static void setSeed(Block block, String seed) {
        setSeed(block, ByteUtil.hexStringToBytes(seed));
    }

    public static void setMiner(Header header, byte[] miner) {
        HexBytes payload = header.getPayload();
        VrfBlockFields vrfBlockFields = getVrfBlockFields(payload);
        vrfBlockFields.setMiner(miner);

        byte[] encoded = RLPCodec.encode(vrfBlockFields);
        header.setPayload(HexBytes.fromBytes(encoded));
    }

    public static void setMiner(Block block, byte[] miner) {
        setMiner(block.getHeader(), miner);
    }

    public static void setMiner(Block block, String miner) {
        setMiner(block, ByteUtil.hexStringToBytes(miner));
    }

    public static void setPriority(Header header, byte[] priority) {
        HexBytes payload = header.getPayload();
        VrfBlockFields vrfBlockFields = getVrfBlockFields(payload);
        vrfBlockFields.setPriority(priority);

        byte[] encoded = RLPCodec.encode(vrfBlockFields);
        header.setPayload(HexBytes.fromBytes(encoded));
    }

    public static void setPriority(Block block, byte[] priority) {
        setPriority(block.getHeader(), priority);
    }

    public static void setPriority(Block block, HexBytes priority) {
        setPriority(block, priority.getBytes());
    }

    public static void setPriority(Block block, String priority) {
        setPriority(block, ByteUtil.hexStringToBytes(priority));
    }

    public static void setProposalProof(Header header, ProposalProof proposalProof) {
        HexBytes payload = header.getPayload();
        VrfBlockFields vrfBlockFields = getVrfBlockFields(payload);
        vrfBlockFields.setProposalProof(RLPCodec.encode(proposalProof));

        byte[] encoded = RLPCodec.encode(vrfBlockFields);
        header.setPayload(HexBytes.fromBytes(encoded));
    }

    public static void setProposalProof(Block block, ProposalProof proposalProof) {
        setProposalProof(block.getHeader(), proposalProof);
    }

    public static VrfBlockFields getVrfBlockFields(HexBytes payload) {
        if (payload == null || payload.size() == 0) {
            return VrfBlockFields.builder().seed(null).priority(null).miner(null).proposalProof(null).build();
        }
        byte[] encoded = payload.getBytes();
        VrfBlockFields vrfBlockFields = RLPCodec.decode(encoded, VrfBlockFields.class);
        return vrfBlockFields;
    }

    // -----> Need to be implemented
//    public static VrfPrivateKey getVrfPrivateKey() {
//        Ed25519PrivateKey skEd25519 = new Ed25519PrivateKey(ByteUtil.hexStringToBytes(VRF_SK));
//        VrfPrivateKey sk = new VrfPrivateKey(skEd25519);
//        return sk;
//    }

    public static VrfPrivateKey getVrfPrivateKey(String vrfDataDir) {
        final String password = "SilkChain@2019@ChangZhou@China#Linux";
        final String vrfPkDir = vrfDataDir + "/keystore";

        PrivateKey key = null;
        FileSystemKeystore fileSystemKeystore = new FileSystemKeystore(vrfPkDir);

        String[] pubkeys = fileSystemKeystore.listStoredKeys();
        if (pubkeys.length == 0) {
            log.info("There is no Vrf SK exist, create a new one and save it in keystore, {}",
                    fileSystemKeystore.getKeyStoreLocation().toAbsolutePath());

            key = new VrfPrivateKey(Ed25519.getAlgorithm()).getSigner();
            fileSystemKeystore.storeKey(key, password);
        } else {
            // We use the first private key as default one
            key = fileSystemKeystore.loadStoredKey(pubkeys[0], password);
            if (key == null) {
                log.error("Fail to load Vrf SK from keystore, create a new one and save it in keystore, {}",
                        fileSystemKeystore.getKeyStoreLocation().toAbsolutePath());

                key = new VrfPrivateKey(Ed25519.getAlgorithm()).getSigner();
                fileSystemKeystore.storeKey(key, password);
            } else {
                log.info("Vrf Sk is loaded from keystore, {}, pubkey {}",
                        fileSystemKeystore.getKeyStoreLocation().toAbsolutePath(), pubkeys[0]);
            }
        }

        return new VrfPrivateKey(key);
    }

    public static VrfPrivateKey getVrfPrivateKey(VrfConfig vrfConfig) {
        return getVrfPrivateKey(vrfConfig.getVrfDataDir());
    }

    public static byte[] genPayload(long blockNum, int round, String seedStr, String minerCoinbaseStr,
            String priorityStr, String blockHashStr, VrfPrivateKey vrfSk, byte[] vrfPk, VrfConfig vrfConfig)
            throws IOException {
        byte[] priority = ByteUtil.hexStringToBytes(priorityStr);
        byte[] seed = ByteUtil.hexStringToBytes(seedStr);
        byte[] blockHash = ByteUtil.hexStringToBytes(blockHashStr);
        byte[] minerCoinbase = ByteUtil.hexStringToBytes(minerCoinbaseStr);
        return genPayload(blockNum, round, seed, minerCoinbase, priority, blockHash, vrfSk, vrfPk, vrfConfig);
    }

    public static byte[] genPayload(long blockNum, int round, byte[] seed, byte[] minerCoinbase, byte[] priority,
            byte[] blockHash, VrfPrivateKey vrfSk, byte[] vrfPk, VrfConfig vrfConfig) throws IOException {
        VrfBlockFields vbf1 = genVrfBlockFields(blockNum, round, seed, minerCoinbase, priority, blockHash, vrfSk, vrfPk,
                vrfConfig);

        byte[] encoded = RLPCodec.encode(vbf1);
        return encoded;
    }

    public static byte[] genPayload(long blockNum, int round, HexBytes seed, HexBytes minerCoinbase, HexBytes priority,
            HexBytes blockHash, VrfPrivateKey vrfSk, byte[] vrfPk, VrfConfig vrfConfig) throws IOException {
        VrfBlockFields vbf1 = genVrfBlockFields(blockNum, round, seed.getBytes(), minerCoinbase.getBytes(),
                priority.getBytes(), blockHash.getBytes(), vrfSk, vrfPk, vrfConfig);

        byte[] encoded = RLPCodec.encode(vbf1);
        return encoded;
    }

    public static VrfBlockFields genVrfBlockFields(long blockNum, int round, String seedStr, String minerCoinbaseStr,
            String priorityStr, String blockHashStr, VrfPrivateKey vrfSk, byte[] vrfPk, VrfConfig vrfConfig)
            throws IOException {
        byte[] priority = ByteUtil.hexStringToBytes(priorityStr);
        byte[] seed = ByteUtil.hexStringToBytes(seedStr);
        byte[] blockHash = ByteUtil.hexStringToBytes(blockHashStr);
        byte[] minerCoinbase = ByteUtil.hexStringToBytes(minerCoinbaseStr);

        return genVrfBlockFields(blockNum, round, seed, minerCoinbase, priority, blockHash, vrfSk, vrfPk, vrfConfig);
    }

    public static VrfBlockFields genVrfBlockFields(long blockNum, int round, byte[] seed, byte[] minerCoinbase,
            byte[] priority, byte[] blockHash, VrfPrivateKey vrfSk, byte[] vrfPk, VrfConfig vrfConfig)
            throws IOException {

        ProposalProof proposalProof = genProposalProof(blockNum, round, seed, minerCoinbase, blockHash, vrfSk, vrfPk);
        CommitProof parentReductionCommitProof = VrfUtil.readParentReductionCommitProof(vrfConfig);
        CommitProof parentFinalCommitProof = VrfUtil.readParentFinalCommitProof(vrfConfig);
        VrfBlockFields vbf1 = VrfBlockFields.builder().seed(seed).priority(priority).miner(minerCoinbase)
                .proposalProof(RLPCodec.encode(proposalProof))
                .parentReductionCommitProof(RLPCodec.encode(parentReductionCommitProof))
                .parentFinalCommitProof(RLPCodec.encode(parentFinalCommitProof)).build();
        return vbf1;
    }

    public static ProposalProof genProposalProof(long blockNum, int round, String seedStr, String minerCoinbaseStr,
            String blockHashStr, VrfPrivateKey vrfSk, byte[] vrfPk) {
        byte[] seed = ByteUtil.hexStringToBytes(seedStr);
        byte[] blockHash = ByteUtil.hexStringToBytes(blockHashStr);
        byte[] minerCoinbase = ByteUtil.hexStringToBytes(minerCoinbaseStr);
        return genProposalProof(blockNum, round, seed, minerCoinbase, blockHash, vrfSk, vrfPk);
    }

    public static ProposalProof genProposalProof(long blockNum, int round, byte[] seed, byte[] minerCoinbase,
            byte[] blockHash, VrfPrivateKey vrfSk, byte[] vrfPk) {
        BlockIdentifier blockIdentifier = new BlockIdentifier(blockHash, blockNum);

        VrfResult vrfResult = VrfProof.Util.prove(VrfProof.ROLE_CODES_PROPOSER, round, vrfSk, seed);
        VrfProof vrfProof = VrfProof.Util.vrfProof(VrfProof.ROLE_CODES_PROPOSER, round, vrfPk, seed, vrfResult);

        ProposalProof proposalProof = new ProposalProof(vrfProof, minerCoinbase, blockIdentifier, vrfSk.getSigner());
        return proposalProof;
    }

    public static CommitProof genCommitlProof(long blockNum, int round, String seedStr, String minerCoinbaseStr,
            String blockHashStr, VrfPrivateKey vrfSk, byte[] vrfPk, int role) {
        byte[] seed = ByteUtil.hexStringToBytes(seedStr);
        byte[] blockHash = ByteUtil.hexStringToBytes(blockHashStr);
        byte[] minerCoinbase = ByteUtil.hexStringToBytes(minerCoinbaseStr);
        return genCommitProof(blockNum, round, seed, minerCoinbase, blockHash, vrfSk, vrfPk, role);
    }

    public static CommitProof genCommitProof(long blockNum, int round, byte[] seed, byte[] minerCoinbase,
            byte[] blockHash, VrfPrivateKey vrfSk, byte[] vrfPk, int role) {
        BlockIdentifier blockIdentifier = new BlockIdentifier(blockHash, blockNum);

        VrfResult vrfResult = VrfProof.Util.prove(role, round, vrfSk, seed);
        VrfProof vrfProof = VrfProof.Util.vrfProof(role, round, vrfPk, seed, vrfResult);

        CommitProof commitProof = new CommitProof(vrfProof, minerCoinbase, blockIdentifier, vrfSk.getSigner());
        return commitProof;
    }

    public static VrfPrivateKey getVrfPrivateKeyDummy() {
        Ed25519PrivateKey skEd25519 = new Ed25519PrivateKey(ByteUtil.hexStringToBytes(VRF_SK));
        VrfPrivateKey sk = new VrfPrivateKey(skEd25519);
        return sk;
    }

    public static byte[] buildMessageBytes(VrfMessageCode code, Object object) {
        return buildMessageBytes(code.ordinal(), object);
    }

    private static byte[] buildMessageBytes(int code, Object object) {
        RLPList list = RLPElement.readRLPTree(object).asRLPList();
        list.add(0, RLPItem.fromInt(code));
        return list.getEncoded();
    }

    @Getter
    public static class VrfMessageCodeAndBytes {
        VrfMessageCode code;
        byte[] rlpBytes;

        public VrfMessageCodeAndBytes(VrfMessageCode code, byte[] rlpBytes) {
            this.code = code;
            this.rlpBytes = rlpBytes;
        }
    }

    public static VrfMessageCodeAndBytes parseMessageBytes(byte[] msg) {
        RLPList list = RLPElement.fromEncoded(msg).asRLPList();
        int codeInt = list.get(0).asInt();
        list = list.subList(1, list.size());
        VrfMessageCode[] vrfMsgCodes = VrfMessageCode.values();
        VrfMessageCode code = vrfMsgCodes[codeInt];
        return new VrfMessageCodeAndBytes(code, list.getEncoded());
    }

    public static String getVrfCacheDir(VrfConfig vrfConfig) {
        // Cache dir is at the same level as keystore.
        String vrfDataDirStr = vrfConfig.getVrfDataDir();
        String vrfCacheDirRel = vrfDataDirStr + "/cache";
        FileSystemKeystore fileSystemKeystore = new FileSystemKeystore(vrfCacheDirRel);
        String vrfCacheDirAbs = fileSystemKeystore.getKeyStoreLocation().toAbsolutePath().toString();
        return vrfCacheDirAbs;
    }

    public static String getReductionCommitProofCachePath(VrfConfig vrfConfig) {
        return getVrfCacheDir(vrfConfig) + File.separator + VrfConstants.REDUCTION_COMMIT_PROOF_FILE_NAME;
    }

    public static String getFinalCommitProofCachePath(VrfConfig vrfConfig) {
        return getVrfCacheDir(vrfConfig) + File.separator + VrfConstants.FINAL_COMMIT_PROOF_FILE_NAME;
    }

    public static CommitProof readParentReductionCommitProof(VrfConfig vrfConfig) throws IOException {
        return readCommitProofFromFile(getReductionCommitProofCachePath(vrfConfig));
    }

    public static CommitProof readParentFinalCommitProof(VrfConfig vrfConfig) throws IOException {
        return readCommitProofFromFile(getFinalCommitProofCachePath(vrfConfig));
    }

    public static CommitProof readCommitProofFromFile(String filePath) throws IOException {
        String proofString = FileUtil.readTxtFile(filePath);
        if (proofString != null) {
            byte[] proofBytes = ByteUtil.hexStringToBytes(proofString);
            CommitProof commitProof = RLPCodec.decode(proofBytes, CommitProof.class);
            return commitProof;
        }
        return null;
    }

    public static void writeCommitProofToFile(CommitProof commitProof, String filePath) throws IOException {
        byte[] encoded = RLPCodec.encode(commitProof);
        File file = new File(filePath);
        file.getParentFile().mkdirs();
        FileUtil.writeTxtFile(ByteUtil.toHexString(encoded), filePath, false, "utf-8");
    }

    public static void writeReductionCommitProofToFile(CommitProof commitProof, VrfConfig vrfConfig)
            throws IOException {
        String filePath = getReductionCommitProofCachePath(vrfConfig);
        writeCommitProofToFile(commitProof, filePath);
    }

    public static void writeFinalCommitProofToFile(CommitProof commitProof, VrfConfig vrfConfig) throws IOException {
        String filePath = getFinalCommitProofCachePath(vrfConfig);
        writeCommitProofToFile(commitProof, filePath);
    }

    public static void setParentReductionCommitProof(Header header, CommitProof parentReductionCommitProof) {
        HexBytes payload = header.getPayload();
        VrfBlockFields vrfBlockFields = getVrfBlockFields(payload);
        vrfBlockFields.setParentReductionCommitProof(RLPCodec.encode(parentReductionCommitProof));

        byte[] encoded = RLPCodec.encode(vrfBlockFields);
        header.setPayload(HexBytes.fromBytes(encoded));
    }

    public static void setParentReductionCommitProof(Block block, CommitProof parentReductionCommitProof) {
        setParentReductionCommitProof(block.getHeader(), parentReductionCommitProof);
    }

    public static void setParentFinalCommitProof(Header header, CommitProof parentFinalCommitProof) {
        HexBytes payload = header.getPayload();
        VrfBlockFields vrfBlockFields = getVrfBlockFields(payload);
        vrfBlockFields.setParentFinalCommitProof(RLPCodec.encode(parentFinalCommitProof));

        byte[] encoded = RLPCodec.encode(vrfBlockFields);
        header.setPayload(HexBytes.fromBytes(encoded));
    }

    public static void setParentFinalCommitProof(Block block, CommitProof parentFinalCommitProof) {
        setParentFinalCommitProof(block.getHeader(), parentFinalCommitProof);
    }

    public static CommitProof getParentReductionCommitProof(HexBytes payload) {
        byte[] encoded = payload.getBytes();
        VrfBlockFields vrfBlockFields = RLPCodec.decode(encoded, VrfBlockFields.class);
        return RLPCodec.decode(vrfBlockFields.getParentReductionCommitProof(), CommitProof.class);
    }

    public static CommitProof getParentReductionCommitProof(Header header) {
        return getParentReductionCommitProof(header.getPayload());
    }

    public static CommitProof getParentReductionCommitProof(Block block) {
        return getParentReductionCommitProof(block.getPayload());
    }

    public static CommitProof getParentFinalCommitProof(HexBytes payload) {
        byte[] encoded = payload.getBytes();
        VrfBlockFields vrfBlockFields = RLPCodec.decode(encoded, VrfBlockFields.class);
        return RLPCodec.decode(vrfBlockFields.getParentFinalCommitProof(), CommitProof.class);
    }

    public static CommitProof getParentFinalCommitProof(Header header) {
        return getParentFinalCommitProof(header.getPayload());
    }

    public static CommitProof getParentFinalCommitProof(Block block) {
        return getParentFinalCommitProof(block.getPayload());
    }
}

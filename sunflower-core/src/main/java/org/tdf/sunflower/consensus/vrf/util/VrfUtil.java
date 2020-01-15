package org.tdf.sunflower.consensus.vrf.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
        String parentReductionCommitProofs = VrfUtil.readParentReductionCommitProofs(vrfConfig);
        String parentFinalCommitProofs = VrfUtil.readParentFinalCommitProofs(vrfConfig);
        VrfBlockFields vbf1 = VrfBlockFields.builder().seed(seed).priority(priority).miner(minerCoinbase)
                .proposalProof(RLPCodec.encode(proposalProof)).parentReductionCommitProofs(parentReductionCommitProofs)
                .parentFinalCommitProofs(parentFinalCommitProofs).build();
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

    public static String readParentReductionCommitProofs(VrfConfig vrfConfig) throws IOException {
        return readCommitProofsFromFile(getReductionCommitProofCachePath(vrfConfig));
    }

    public static String readParentFinalCommitProofs(VrfConfig vrfConfig) throws IOException {
        return readCommitProofsFromFile(getFinalCommitProofCachePath(vrfConfig));
    }

    public static String readCommitProofsFromFile(String filePath) throws IOException {
        String proofsString = FileUtil.readTxtFile(filePath);
        return proofsString;
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

    public static void setParentReductionCommitProofs(Header header, String parentReductionCommitProofs) {
        HexBytes payload = header.getPayload();
        VrfBlockFields vrfBlockFields = getVrfBlockFields(payload);
        vrfBlockFields.setParentReductionCommitProofs(parentReductionCommitProofs);

        byte[] encoded = RLPCodec.encode(vrfBlockFields);
        header.setPayload(HexBytes.fromBytes(encoded));
    }

    public static void setParentReductionCommitProofs(Block block, String parentReductionCommitProofs) {
        setParentReductionCommitProofs(block.getHeader(), parentReductionCommitProofs);
    }

    public static void setParentFinalCommitProofs(Header header, String parentFinalCommitProofs) {
        HexBytes payload = header.getPayload();
        VrfBlockFields vrfBlockFields = getVrfBlockFields(payload);
        vrfBlockFields.setParentFinalCommitProofs(parentFinalCommitProofs);

        byte[] encoded = RLPCodec.encode(vrfBlockFields);
        header.setPayload(HexBytes.fromBytes(encoded));
    }

    public static void setParentFinalCommitProofs(Block block, String parentFinalCommitProofs) {
        setParentFinalCommitProofs(block.getHeader(), parentFinalCommitProofs);
    }

    public static String getParentReductionCommitProofs(HexBytes payload) {
        byte[] encoded = payload.getBytes();
        VrfBlockFields vrfBlockFields = RLPCodec.decode(encoded, VrfBlockFields.class);
        return vrfBlockFields.getParentReductionCommitProofs();
    }

    public static String getParentReductionCommitProofs(Header header) {
        return getParentReductionCommitProofs(header.getPayload());
    }

    public static String getParentReductionCommitProofs(Block block) {
        return getParentReductionCommitProofs(block.getPayload());
    }

    public static String getParentFinalCommitProofs(HexBytes payload) {
        byte[] encoded = payload.getBytes();
        VrfBlockFields vrfBlockFields = RLPCodec.decode(encoded, VrfBlockFields.class);
        return vrfBlockFields.getParentFinalCommitProofs();
    }

    public static String getParentFinalCommitProofs(Header header) {
        return getParentFinalCommitProofs(header.getPayload());
    }

    public static String getParentFinalCommitProofs(Block block) {
        return getParentFinalCommitProofs(block.getPayload());
    }

    /**
     * @param proofsString: comma separated string, each item is a RLP encoded bytes
     *                      string of CommitProof.
     * @return
     */
    public static List<CommitProof> parseCommitProofs(String proofsString) {
        if (proofsString == null) {
            return null;
        }
        String[] proofArrayStr = proofsString.split(",");
        ArrayList<CommitProof> proofList = new ArrayList<>();
        for (int i = 0; i < proofArrayStr.length; i++) {
            byte[] data = ByteUtil.hexStringToBytes(proofArrayStr[i]);
            CommitProof commitProof = RLPCodec.decode(data, CommitProof.class);
            proofList.add(commitProof);
        }
        return proofList;
    }

    public static List<CommitProof> getParentReductionCommitProofList(HexBytes payload) {
        return parseCommitProofs(getParentReductionCommitProofs(payload));
    }

    public static List<CommitProof> getParentReductionCommitProofList(Header header) {
        return getParentReductionCommitProofList(header.getPayload());
    }

    public static List<CommitProof> getParentReductionCommitProofList(Block block) {
        return getParentReductionCommitProofList(block.getPayload());
    }

    public static List<CommitProof> getParentFinalCommitProofList(HexBytes payload) {
        return parseCommitProofs(getParentReductionCommitProofs(payload));
    }

    public static List<CommitProof> getParentFinalCommitProofList(Header header) {
        return getParentFinalCommitProofList(header.getPayload());
    }

    public static List<CommitProof> getParentFinalCommitProofList(Block block) {
        return getParentFinalCommitProofList(block.getPayload());
    }

    public static String commitProofListToCommaSepStr(List<CommitProof> list) {
        if (list == null) {
            return null;
        }

        StringBuffer tmp = new StringBuffer();
        int size = list.size();
        for (int i = 0; i < size; i++) {
            tmp.append(ByteUtil.toHexString(RLPCodec.encode(list.get(i)))).append(',');
        }
        if (tmp.length() > 0) {
            tmp.deleteCharAt(tmp.length() - 1);
        }
        return tmp.toString();
    }
}

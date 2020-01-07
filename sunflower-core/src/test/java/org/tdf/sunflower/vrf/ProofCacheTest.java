package org.tdf.sunflower.vrf;

import java.io.File;

import org.junit.Test;
import org.tdf.rlp.RLPCodec;
import org.tdf.sunflower.consensus.vrf.core.CommitProof;
import org.tdf.sunflower.consensus.vrf.struct.VrfPrivateKey;
import org.tdf.sunflower.consensus.vrf.util.FileUtil;
import org.tdf.sunflower.consensus.vrf.util.VrfUtil;
import org.tdf.sunflower.util.ByteUtil;

public class ProofCacheTest {
    int round = 1;
    long blockNum = 2;
    String nonceStr = "abcd";
    String priorityStr = "cdef";
    String blockHashStr = "abcdef";
    String minerCoinbaseStr = "0123456789abcdef";
    VrfPrivateKey vrfSk = VrfUtil.getVrfPrivateKeyDummy();
    byte[] vrfPk = vrfSk.generatePublicKey().getEncoded();

    public ProofCacheTest() {
        // TODO Auto-generated constructor stub
    }

    @Test
    public void testCommitProofRlp() {
        // !!! Following hex string length should be even number.
        CommitProof commitProof = VrfUtil.genCommitlProof(blockNum, round, nonceStr, minerCoinbaseStr, blockHashStr,
                vrfSk, vrfPk);

        byte[] encoded = RLPCodec.encode(commitProof);

        CommitProof commitProof2 = RLPCodec.decode(encoded, CommitProof.class);
        assert (ByteUtil.toHexString(commitProof2.getVrfProof().getSeed()).equals(nonceStr));
        assert (ByteUtil.toHexString(commitProof2.getCoinbase()).equals(minerCoinbaseStr));
        assert (commitProof2.getRound() == round);
        assert (ByteUtil.toHexString(commitProof2.getBlockIdentifier().getHash()).equals(blockHashStr));
        assert (commitProof2.getBlockIdentifier().getNumber() == blockNum);
        assert (ByteUtil.toHexString(commitProof2.getCoinbase()).equals(minerCoinbaseStr));
        assert (ByteUtil.toHexString(commitProof2.getVrfPk()).equals(ByteUtil.toHexString(vrfPk)));
    }

    @Test
    public void testCommitProofStorage() throws Exception {
        // !!! Following hex string length should be even number.

        CommitProof commitProof = VrfUtil.genCommitlProof(blockNum, round, nonceStr, minerCoinbaseStr, blockHashStr,
                vrfSk, vrfPk);

        byte[] encoded = RLPCodec.encode(commitProof);
        File tmpDir = new File("tmp");
        if (!tmpDir.exists()) {
            tmpDir.mkdirs();
        }

        String filePath = tmpDir.getAbsolutePath() + File.separator + "proof.txt";
        FileUtil.writeTxtFile(ByteUtil.toHexString(encoded), filePath, false, "utf-8");

        String byteStr = FileUtil.readTxtFile(filePath);
        byte[] bytesFromFile = ByteUtil.hexStringToBytes(byteStr);
        CommitProof commitProof2 = RLPCodec.decode(bytesFromFile, CommitProof.class);
        assert (ByteUtil.toHexString(commitProof2.getVrfProof().getSeed()).equals(nonceStr));
        assert (ByteUtil.toHexString(commitProof2.getCoinbase()).equals(minerCoinbaseStr));
        assert (commitProof2.getRound() == round);
        assert (ByteUtil.toHexString(commitProof2.getBlockIdentifier().getHash()).equals(blockHashStr));
        assert (commitProof2.getBlockIdentifier().getNumber() == blockNum);
        assert (ByteUtil.toHexString(commitProof2.getCoinbase()).equals(minerCoinbaseStr));
        assert (ByteUtil.toHexString(commitProof2.getVrfPk()).equals(ByteUtil.toHexString(vrfPk)));
    }
}

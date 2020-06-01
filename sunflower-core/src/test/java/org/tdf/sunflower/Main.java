package org.tdf.sunflower;

import org.tdf.common.util.HexBytes;
import org.tdf.crypto.sm2.SM2;
import org.tdf.crypto.sm2.SM2PrivateKey;
import org.tdf.crypto.sm2.SM2PublicKey;
import org.tdf.sunflower.consensus.pos.PoS;
import org.tdf.sunflower.crypto.CryptoHelpers;
import org.tdf.sunflower.state.Constants;
import org.tdf.sunflower.types.CryptoContext;
import org.tdf.sunflower.types.Transaction;

public class Main {
    static {
        CryptoContext.setSignatureVerifier((pk, msg, sig) -> new SM2PublicKey(pk).verify(msg, sig));
        CryptoHelpers.signer = (sk, msg) -> new SM2PrivateKey(sk).sign(msg);
        CryptoHelpers.generateKeyPair = SM2::generateKeyPair;
        CryptoHelpers.getPkFromSk = (sk) -> new SM2PrivateKey(sk).generatePublicKey().getEncoded();
        CryptoHelpers.ecdh = (initiator, sk, pk) -> SM2.calculateShareKey(initiator, sk, sk, pk, pk, "userid@soie-chain.com".getBytes());
    }

    private static final HexBytes FROM_SK = HexBytes.fromHex("");

    public static void main(String[] args) throws Exception{
        Transaction v = new Transaction(
                PoS.TRANSACTION_VERSION,
                Transaction.Type.CONTRACT_CALL.code,
                System.currentTimeMillis() / 1000,
                1,
                HexBytes.fromBytes(CryptoHelpers.getPkFromSk(FROM_SK.getBytes())),
                0,
                0,
                HexBytes.fromHex("00").concat(HexBytes.fromHex("9210f54ee868d4744bf1f00ea90d062e328f05962807972659443f53338c8941")),
                Constants.POS_AUTHENTICATION_ADDR,
                HexBytes.fromHex("ff")
        );

        byte[] sig = CryptoHelpers.sign(FROM_SK.getBytes(), v.getSignaturePlain());
        v.setSignature(HexBytes.fromBytes(sig));
        System.out.println(Start.MAPPER.writeValueAsString(v));
    }
}

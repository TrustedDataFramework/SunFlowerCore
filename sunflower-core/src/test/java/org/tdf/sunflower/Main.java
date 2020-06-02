package org.tdf.sunflower;

import org.tdf.common.util.HexBytes;
import org.tdf.crypto.sm2.SM2;
import org.tdf.crypto.sm2.SM2PrivateKey;
import org.tdf.crypto.sm2.SM2PublicKey;
import org.tdf.sunflower.consensus.poa.PoAConstants;
import org.tdf.sunflower.crypto.CryptoHelpers;
import org.tdf.sunflower.state.Constants;
import org.tdf.sunflower.types.CryptoContext;
import org.tdf.sunflower.types.Transaction;

public class Main {
    static {
        CryptoContext.setSignatureVerifier((pk, msg, sig) -> new SM2PublicKey(pk).verify(msg, sig));
        CryptoContext.setSigner((sk, msg) -> new SM2PrivateKey(sk).sign(msg));
        CryptoHelpers.generateKeyPair = SM2::generateKeyPair;
        CryptoContext.setGetPkFromSk((sk) -> new SM2PrivateKey(sk).generatePublicKey().getEncoded());
        CryptoHelpers.ecdh = (initiator, sk, pk) -> SM2.calculateShareKey(initiator, sk, sk, pk, pk, "userid@soie-chain.com".getBytes());
    }

    private static final HexBytes FROM_SK = HexBytes.fromHex("f00df601a78147ffe0b84de1dffbebed2a6ea965becd5d0bd7faf54f1f29c6b5");

    public static void main(String[] args) throws Exception{
        Transaction v = new Transaction(
                PoAConstants.TRANSACTION_VERSION,
                Transaction.Type.CONTRACT_CALL.code,
                System.currentTimeMillis() / 1000,
                1,
                HexBytes.fromBytes(CryptoContext.getPkFromSk(FROM_SK.getBytes())),
                0,
                0,
                HexBytes.fromHex("01").concat(HexBytes.fromHex("bf0aba026e5a0e1a69094c8a0d19d905367d64cf")),
                Constants.PEER_AUTHENTICATION_ADDR,
                HexBytes.fromHex("ff")
        );

        byte[] sig = CryptoContext.sign(FROM_SK.getBytes(), v.getSignaturePlain());
        v.setSignature(HexBytes.fromBytes(sig));
        System.out.println(Start.MAPPER.writeValueAsString(v));
    }
}

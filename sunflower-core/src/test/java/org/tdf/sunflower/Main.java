package org.tdf.sunflower;

import lombok.SneakyThrows;
import org.tdf.common.util.HexBytes;
import org.tdf.crypto.sm2.SM2;
import org.tdf.crypto.sm2.SM2PrivateKey;
import org.tdf.crypto.sm2.SM2PublicKey;
import org.tdf.gmhelper.SM2Util;
import org.tdf.sunflower.consensus.poa.PoAConstants;
import org.tdf.crypto.CryptoHelpers;
import org.tdf.sunflower.facade.SecretStoreImpl;
import org.tdf.sunflower.state.Constants;
import org.tdf.sunflower.types.CryptoContext;
import org.tdf.sunflower.types.Transaction;

public class Main {
    static {
        CryptoContext.setSignatureVerifier((pk, msg, sig) -> new SM2PublicKey(pk).verify(msg, sig));
        CryptoContext.setSigner((sk, msg) -> new SM2PrivateKey(sk).sign(msg));
        CryptoContext.setSecretKeyGenerator(() -> SM2.generateKeyPair().getPrivateKey().getEncoded());
        CryptoContext.setGetPkFromSk((sk) -> new SM2PrivateKey(sk).generatePublicKey().getEncoded());
        CryptoContext.setEcdh((initiator, sk, pk) -> SM2.calculateShareKey(initiator, sk, sk, pk, pk, SM2Util.WITH_ID));
        CryptoContext.setEncrypt(CryptoHelpers.ENCRYPT);
        CryptoContext.setDecrypt(CryptoHelpers.DECRYPT);
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

        printSecretStore();
    }

    @SneakyThrows
    public static void printSecretStore(){
        byte[] aliceSk = CryptoContext.generateSecretKey();
        System.out.print("alice sk = " + HexBytes.fromBytes(aliceSk));
        byte[] alicePk = CryptoContext.getPkFromSk(aliceSk);
        byte[] bobPk = HexBytes.decode("03cac34009c85674f46f0801d195a216030807f6aa2be337e754ae7645bf7a1106");
        byte[] key = CryptoContext.ecdh(true, aliceSk, bobPk);
        byte[] plain = HexBytes.decode("f00df601a78147ffe0b84de1dffbebed2a6ea965becd5d0bd7faf54f1f29c6b5");
        byte[] cipher = CryptoContext.encrypt(key, plain);
        SecretStoreImpl s = new SecretStoreImpl(
                HexBytes.fromBytes(alicePk),
                HexBytes.fromBytes(cipher)
        );
        System.out.println(Start.MAPPER.writeValueAsString(s));
    }
}

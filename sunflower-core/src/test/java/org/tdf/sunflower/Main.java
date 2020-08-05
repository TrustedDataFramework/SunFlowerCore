package org.tdf.sunflower;

import lombok.SneakyThrows;
import org.tdf.common.util.BigEndian;
import org.tdf.common.util.HexBytes;
import org.tdf.common.util.LittleEndian;
import org.tdf.crypto.sm2.SM2;
import org.tdf.crypto.sm2.SM2PrivateKey;
import org.tdf.crypto.sm2.SM2PublicKey;
import org.tdf.gmhelper.SM2Util;
import org.tdf.gmhelper.SM3Util;
import org.tdf.sunflower.consensus.poa.PoAConstants;
import org.tdf.crypto.CryptoHelpers;
import org.tdf.sunflower.facade.SecretStoreImpl;
import org.tdf.sunflower.state.Constants;
import org.tdf.sunflower.state.PreBuiltContract;
import org.tdf.sunflower.types.CryptoContext;
import org.tdf.sunflower.types.Transaction;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Main {
    static {
        CryptoContext.setSignatureVerifier((pk, msg, sig) -> new SM2PublicKey(pk).verify(msg, sig));
        CryptoContext.setSigner((sk, msg) -> new SM2PrivateKey(sk).sign(msg));
        CryptoContext.setSecretKeyGenerator(() -> SM2.generateKeyPair().getPrivateKey().getEncoded());
        CryptoContext.setGetPkFromSk((sk) -> new SM2PrivateKey(sk).generatePublicKey().getEncoded());
        CryptoContext.setEcdh((initiator, sk, pk) -> SM2.calculateShareKey(initiator, sk, sk, pk, pk, SM2Util.WITH_ID));
        CryptoContext.setEncrypt(CryptoHelpers.ENCRYPT);
        CryptoContext.setDecrypt(CryptoHelpers.DECRYPT);
        CryptoContext.setPublicKeySize(CryptoContext.getPkFromSk(CryptoContext.generateSecretKey()).length);
        CryptoContext.setHashFunction(SM3Util::hash);
    }

    public static final int REVERSED_ADDRESSES_START_INDEX = 8;
    public static final int REVERSED_ADDRESSES_END_INDEX = 8 + 65536;

    public static List<HexBytes> getReversedContracts() {
        List<HexBytes> ret = new ArrayList<>();
        for (long i = REVERSED_ADDRESSES_START_INDEX; i < REVERSED_ADDRESSES_END_INDEX; i++) {
            byte[] addr = BigEndian.encodeUint256(BigInteger.valueOf(i));
            addr = Arrays.copyOfRange(addr, addr.length - 20, addr.length);

            ret.add(HexBytes.fromBytes(addr));
        }
        return ret;
    }


    private static final HexBytes FROM_SK = HexBytes.fromHex("f00df601a78147ffe0b84de1dffbebed2a6ea965becd5d0bd7faf54f1f29c6b5");

    public static void main(String[] args) throws Exception{
       SM2PrivateKey sk = new SM2PrivateKey(HexBytes.decode("f00df601a78147ffe0b84de1dffbebed2a6ea965becd5d0bd7faf54f1f29c6b5"));
       SM2PublicKey pk = (SM2PublicKey) sk.generatePublicKey();
       byte[] msg = sk.decrypt(HexBytes.decode("44e004e48a31a6abc1cb687dee9368fa5c28e42302e7536f6a2cc5a758f1f9cc3fae860a57a0eb8071a01abbc76f40bf0a6b9a0df459febf6c681f79f27964d9306139660a16fb0c6e32fe51a26abdde95764bd888b3c9746e9989f4feef1eef83ef5b"));
        for (byte b : msg) {
            System.out.println(Byte.toUnsignedInt(b));
        }
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

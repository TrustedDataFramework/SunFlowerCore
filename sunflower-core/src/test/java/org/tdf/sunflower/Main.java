package org.tdf.sunflower;

import lombok.SneakyThrows;
import org.bouncycastle.math.ec.ECPoint;
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
import java.nio.charset.StandardCharsets;
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
        String pkHex = "041a6b8a4942f7b3920b58b63b8f2027f3b2c4f05e56f9341810aea397a996ca7e71d3b73e4b77d3831341c9ef7ae9bc045f0ce2f08391a371b7485903c0374cc1";
        byte[] enc = SM2Util.encrypt(HexBytes.decode(pkHex), "123".getBytes());
        System.out.println(HexBytes.encode(enc));

        String skHex = "0d332ed4ef2c25cdba54057f5dbf706a56216664f944eaa95474e8f950344c52";
        byte[] decrypted = SM2Util.decrypt(HexBytes.decode(skHex), HexBytes.decode("6226f131912c25d6a1d0c8d20f19f87f27225633cbfe325d2998b77d02ad7e013a9afd3c6e55120eec91e14eeaacd86d9eba514b9c5abbd02d0ce1b1225e48e3e9bff6894304651da5bd55f10913120481677f8477a725c2fdfafe8f4d0ffa60b132c467c96fc0"));
        System.out.println(new String(decrypted, StandardCharsets.US_ASCII));
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

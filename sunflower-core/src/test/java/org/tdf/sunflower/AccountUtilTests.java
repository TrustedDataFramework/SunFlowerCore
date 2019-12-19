package org.tdf.sunflower;

import org.apache.commons.codec.DecoderException;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.account.Utils;

import java.util.Arrays;

@RunWith(JUnit4.class)
public class AccountUtilTests {
    private static HexBytes PUBLIC_KEY = HexBytes.fromHex("d0f1966cee219fcfdbcee698517fcf864f46817c30bc8218eb4889d02f312540");


    @Test
    public void test() {
        assert Arrays.equals(
                Utils.publicKeyToHash(PUBLIC_KEY.getBytes()),
                Hex.decode("039a676b65273eeca96af35e35c05e482650c979")
        );

        assert Utils.publicKeyToAddress(PUBLIC_KEY.getBytes())
                .equals("1L3zkde4kSpfd1L7NYmNYSBf1Bvh6fZLk");

        assert Utils.publicKeyHashToAddress(Utils.addressToPublicKeyHash("1L3zkde4kSpfd1L7NYmNYSBf1Bvh6fZLk").get()).equals(
                "1L3zkde4kSpfd1L7NYmNYSBf1Bvh6fZLk"
        );

        assert Arrays.equals(
                Utils.addressToPublicKeyHash("1L3zkde4kSpfd1L7NYmNYSBf1Bvh6fZLk").get(),
                Hex.decode("039a676b65273eeca96af35e35c05e482650c979")
        );
    }
}

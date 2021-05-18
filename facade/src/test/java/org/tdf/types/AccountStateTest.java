package org.tdf.types;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.spongycastle.util.encoders.Hex;
import org.tdf.common.types.Uint256;
import org.tdf.rlpstream.Rlp;
import org.tdf.sunflower.state.Account;

import java.math.BigInteger;

import static org.junit.Assert.assertEquals;

@RunWith(JUnit4.class)
public class AccountStateTest {

    @Test
    public void testGetEncoded() {
        String expected = "f85e809"
            + "a0100000000000000000000000000000000000000000000000000"
            + "a056e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421"
            + "a0c5d2460186f7233c927e7db2dcc703c0e500b653ca82273b7bfad8045d85a470";
        Account acct = Account.emptyAccount(Uint256.of(BigInteger.valueOf(2).pow(200)));
        assertEquals(expected, Hex.toHexString(Rlp.encode(acct)));
    }

}


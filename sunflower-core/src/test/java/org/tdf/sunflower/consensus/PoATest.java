package org.tdf.sunflower.consensus;

import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.Start;
import org.tdf.sunflower.consensus.poa.PoAConstants;
import org.tdf.sunflower.state.Constants;
import org.tdf.sunflower.types.Transaction;

public class PoATest {
    public static void main(String[] args) throws Exception{
        Transaction t = new Transaction(
                PoAConstants.TRANSACTION_VERSION,
                Transaction.Type.CONTRACT_CALL.code,
                System.currentTimeMillis() / 1000,
                1,
                HexBytes.fromHex("00"),
                0,
                0,
                HexBytes.fromHex("01").concat(HexBytes.fromHex("00")),
                Constants.AUTHENTICATION_ADDR,
                HexBytes.fromHex("ff")
        );

        System.out.println(Start.MAPPER.writeValueAsString(t));
    }
}

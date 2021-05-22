package org.tdf.sunflower;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.p2pv2.message.Message;
import org.tdf.sunflower.p2pv2.p2p.HelloMessage;
import org.tdf.sunflower.p2pv2.p2p.P2PMessageFactory;

@RunWith(JUnit4.class)
public class P2pMessageFactoryTest {

    @Test
    public void test0() {
        int type = 0;
        String bin = "f8540580ccc5836574683ec5836574683f822161b840db477274e444681881172d4a7ce2ff1d00403e5dd6e8d9341c6fb7a448115755665ccef8acc7da3c56d7d0949ae22cf7bc19fad8ffbe3563e02356be834502cb";

        Message msg = P2PMessageFactory.INSTANCE
            .create(type, HexBytes.decode(bin));

        assert msg instanceof HelloMessage;
        HelloMessage m = (HelloMessage) msg;

        assert m.getListenPort() == 8545;
    }
}

package org.tdf.sunflower.vrf;

import org.junit.Test;
import org.tdf.sunflower.consensus.vrf.struct.VrfPrivateKey;
import org.tdf.sunflower.consensus.vrf.util.VrfUtil;
import org.tdf.sunflower.util.ByteUtil;

public class VrfKeyTest {

    public VrfKeyTest() {

    }
    
    @Test
    public void testVrfSk()
    {
        VrfPrivateKey sk = VrfUtil.getVrfPrivateKey();
        assert(ByteUtil.toHexString(sk.generatePublicKey().getEncoded()).equals(VrfUtil.VRF_PK));
        assert(ByteUtil.toHexString(sk.getEncoded()).equals(VrfUtil.VRF_SK));
    }
}

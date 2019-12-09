package org.tdf.sunflower.vrf;

import org.junit.Test;
import org.tdf.sunflower.consensus.vrf.struct.VrfPrivateKey;
import org.tdf.sunflower.consensus.vrf.util.VrfUtil;
import org.tdf.sunflower.util.ByteUtil;

public class VrfKeyTest {

    public VrfKeyTest() {

    }
    
    @Test
    public void testVrfSkKeyStore()
    {
        VrfUtil.getVrfPrivateKey("vrf-data1");
    }
}

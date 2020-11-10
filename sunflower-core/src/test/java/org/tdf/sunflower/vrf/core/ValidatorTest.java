/*
 * Copyright (c) [2019] [ <silk chain> ]
 * This file is part of the silk chain library.
 *
 * The silk chain library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The silk chain library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the silk chain library. If not, see <http://www.gnu.org/licenses/>.
 */
package org.tdf.sunflower.vrf.core;

import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;
import org.tdf.crypto.PrivateKey;
import org.tdf.crypto.ed25519.Ed25519PrivateKey;
import org.tdf.sunflower.consensus.vrf.core.Validator;
import org.tdf.sunflower.consensus.vrf.struct.VrfPrivateKey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ValidatorTest {

    private static final String privateKeyHex = "09bb524717b97f0ea5684962ccea964216483157a8170070927bd01c6913d823";
    private static final byte[] coinbase = Hex.decode("6dE5E8820def9F49BBd01EC07fFc2D931CD15a85");

    @Test
    public void testRlpEncode() {
        PrivateKey privateKey = new Ed25519PrivateKey(Hex.decode(privateKeyHex));
        VrfPrivateKey sk = new VrfPrivateKey(privateKey);

        byte[] vrfPk = sk.generatePublicKey().getEncoded();

        Validator validator1 = new Validator(coinbase, 100, vrfPk);
        byte[] rlpEncoded = validator1.getEncoded();

        Validator validator2 = new Validator(rlpEncoded);

        assertEquals(Hex.toHexString(validator1.getVrfPk()), Hex.toHexString(validator2.getVrfPk()));
        assertEquals(Hex.toHexString(validator1.getCoinbase()), Hex.toHexString(validator2.getCoinbase()));
        assertTrue(validator1.getDeposit() == validator2.getDeposit());
    }
}
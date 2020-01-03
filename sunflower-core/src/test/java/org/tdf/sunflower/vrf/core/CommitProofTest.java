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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.bouncycastle.util.encoders.Hex;
import org.junit.Ignore;
import org.junit.Test;
import org.tdf.crypto.PrivateKey;
import org.tdf.crypto.ed25519.Ed25519;
import org.tdf.crypto.ed25519.Ed25519PrivateKey;
import org.tdf.sunflower.consensus.vrf.HashUtil;
import org.tdf.sunflower.consensus.vrf.core.BlockIdentifier;
import org.tdf.sunflower.consensus.vrf.core.CommitProof;
import org.tdf.sunflower.consensus.vrf.core.ProposalProof;
import org.tdf.sunflower.consensus.vrf.core.VrfProof;
import org.tdf.sunflower.consensus.vrf.struct.VrfPrivateKey;
import org.tdf.sunflower.consensus.vrf.struct.VrfResult;

public class CommitProofTest {

    private static final String privateKeyHex = "09bb524717b97f0ea5684962ccea964216483157a8170070927bd01c6913d823";

    private static final byte[] coinbase = Hex.decode("6dE5E8820def9F49BBd01EC07fFc2D931CD15a85");

    private static final byte[] seed = "We are the free man in the world, but we try to test the world with our code"
            .getBytes();

    @Test
    public void testRlpEncode() {
        PrivateKey privateKey = new Ed25519PrivateKey(Hex.decode(privateKeyHex));
        VrfPrivateKey vrfSk = new VrfPrivateKey(privateKey);

        byte[] vrfPk = vrfSk.generatePublicKey().getEncoded();
        // Must use VrfProve Util to prove with Role Code
        VrfResult vrfResult = VrfProof.Util.prove(VrfProof.ROLE_CODES_REDUCTION_COMMIT, 0, vrfSk, seed);
        VrfProof vrfProof = VrfProof.Util.vrfProof(VrfProof.ROLE_CODES_REDUCTION_COMMIT, 0, vrfPk, seed, vrfResult);

        System.out.println("private key= " + Hex.toHexString(vrfSk.getEncoded()));
        System.out.println("public key= " + Hex.toHexString(vrfSk.generatePublicKey().getEncoded()));

        BlockIdentifier blockIdentifier = new BlockIdentifier(HashUtil.sha3(coinbase), 1000);
        CommitProof prove1 = new CommitProof(vrfProof, coinbase, blockIdentifier, vrfSk.getSigner());
        byte[] rlpEncoded = prove1.getEncoded();

        ProposalProof prove2 = new ProposalProof(rlpEncoded);

        assertEquals(Hex.toHexString(prove1.getVrfProof().getVrfPk()),
                Hex.toHexString(prove2.getVrfProof().getVrfPk()));
        assertEquals(Hex.toHexString(prove1.getVrfProof().getSeed()), Hex.toHexString(prove2.getVrfProof().getSeed()));

        assertEquals(Hex.toHexString(prove1.getVrfProof().getVrfResult().getR()),
                Hex.toHexString(prove2.getVrfProof().getVrfResult().getR()));
        assertEquals(Hex.toHexString(prove1.getVrfProof().getVrfResult().getProof()),
                Hex.toHexString(prove2.getVrfProof().getVrfResult().getProof()));

        assertEquals(Hex.toHexString(prove1.getCoinbase()), Hex.toHexString(prove2.getCoinbase()));

        assertTrue(prove1.getBlockIdentifier().getNumber() == prove2.getBlockIdentifier().getNumber());
        assertEquals(Hex.toHexString(prove1.getBlockIdentifier().getHash()),
                Hex.toHexString(prove2.getBlockIdentifier().getHash()));

        assertTrue(prove2.verify());
    }

    @Ignore
    @Test
    public void testPriority() {
        final int expected = 26;
        final int weight = 20;
        final int totalWeight = 1000;
        final int nodeCount = totalWeight / weight;

        int loop = 100;
        while (loop-- > 0) {
            int proposers = 0;
            int priorities = 0;
            int weights = 0;

            for (int i = 0; i < nodeCount; ++i) {
                VrfPrivateKey vrfSk = new VrfPrivateKey(Ed25519.getAlgorithm());
                byte[] vrfPk = vrfSk.generatePublicKey().getEncoded();

                // Must use VrfProve Util to prove with Role Code
                VrfResult vrfResult = VrfProof.Util.prove(VrfProof.ROLE_CODES_REDUCTION_COMMIT, 0, vrfSk, seed);
                VrfProof vrfProof = VrfProof.Util.vrfProof(VrfProof.ROLE_CODES_REDUCTION_COMMIT, 0, vrfPk, seed,
                        vrfResult);

                BlockIdentifier blockIdentifier = new BlockIdentifier(HashUtil.sha3(coinbase), 1000);
                CommitProof prove1 = new CommitProof(vrfProof, coinbase, blockIdentifier, vrfSk.getSigner());
                byte[] rlpEncoded = prove1.getEncoded();

                ProposalProof prove2 = new ProposalProof(rlpEncoded);

                int priority = prove2.getPriority(expected, weight, totalWeight);
                // System.out.println("priority= " + priority);

                if (priority > 0) {
                    ++proposers;
                    priorities += priority;
                    weights += priority * weight;
                }

                assert priority < weight;
            }
            System.out.println("proposers= " + proposers + ", priorities= " + priorities + ", weights= " + weights);

            assert proposers > 1;
            // assert weights > (totalWeight / 2);
        }
    }
}
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

import java.security.SecureRandom;

import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;
import org.tdf.common.util.HexBytes;
import org.tdf.crypto.PrivateKey;
import org.tdf.crypto.ed25519.Ed25519;
import org.tdf.crypto.ed25519.Ed25519PrivateKey;
import org.tdf.sunflower.consensus.vrf.HashUtil;
import org.tdf.sunflower.consensus.vrf.core.VrfProof;
import org.tdf.sunflower.consensus.vrf.struct.VrfPrivateKey;
import org.tdf.sunflower.consensus.vrf.struct.VrfResult;
import org.tdf.sunflower.consensus.vrf.util.VrfUtil;
import org.tdf.sunflower.consensus.vrf.vm.DataWord;
import org.tdf.sunflower.types.Header;

public class VrfProofTest {

    private static final String privateKeyHex = "09bb524717b97f0ea5684962ccea964216483157a8170070927bd01c6913d823";

    private static final byte[] coinbase = Hex.decode("6dE5E8820def9F49BBd01EC07fFc2D931CD15a85");

    private static final byte[] seed = HashUtil
            .sha256("We are the free man in the world, but we try to test the world with our code".getBytes());

    private static final byte[] coinbase0 = org.spongycastle.util.encoders.Hex
            .decode("3a0b32b4e6f404934d098957d200e803239fdf75");
    private static final byte[] coinbase1 = org.spongycastle.util.encoders.Hex
            .decode("CD2A3D9F938E13CD947EC05ABC7FE734DF8DD826");
    private static final byte[] coinbase2 = org.spongycastle.util.encoders.Hex
            .decode("13978AEE95F38490E9769C39B2773ED763D9CD5F");
    private static final byte[] coinbase3 = org.spongycastle.util.encoders.Hex
            .decode("DE1E758511A7C67E7DB93D1C23C1060A21DB4615");
    private static final byte[] coinbase4 = org.spongycastle.util.encoders.Hex
            .decode("27DC8DE9E9A1CB673543BD5FCE89E83AF09E228F");
    private static final byte[] coinbase5 = org.spongycastle.util.encoders.Hex
            .decode("D64A66C28A6AE5150AF5E7C34696502793B91AE7");
    private static final byte[] coinbase6 = org.spongycastle.util.encoders.Hex
            .decode("7da1291da8b73c42c9f6307b05e03b8a2663e503");
    private static final byte[] coinbase7 = org.spongycastle.util.encoders.Hex
            .decode("6eed625f205171dde6192336dd4b5f59276742fb");
    private static final byte[] coinbase8 = org.spongycastle.util.encoders.Hex
            .decode("003d9f0d826e357fea013b37b0f988a1a87e03f0");
    private static final byte[] coinbase9 = org.spongycastle.util.encoders.Hex
            .decode("3551b14081dc0fd629671114f49d332f059e0cba");

    private static final byte[][] COINBASE_ARRAY = new byte[][] { coinbase0, coinbase1, coinbase2, coinbase3, coinbase4,
            coinbase5, coinbase6, coinbase7, coinbase8, coinbase9 };

    private static Header createBlockHeaders(int index, byte[] coinbase) {
        byte[] emptyArray = new byte[0];
        byte[] recentHash = emptyArray;
        // Compose new nonce with coinbase and index and initialize new block header
        byte[] nonce = new byte[coinbase.length + 4];
        System.arraycopy(coinbase, 0, nonce, 0, coinbase.length);
        nonce[coinbase.length] = (byte) ((index >> 24) & 0xFF);
        nonce[coinbase.length + 1] = (byte) ((index >> 16) & 0xFF);
        nonce[coinbase.length + 2] = (byte) ((index >> 8) & 0xFF);
        nonce[coinbase.length + 3] = (byte) (index & 0xFF);
        long time = System.currentTimeMillis() / 1000;

        Header header = Header.builder().hashPrev(HexBytes.fromBytes(recentHash)).createdAt(System.currentTimeMillis())
                .version(1).height(index).build();
        VrfUtil.setNonce(header, nonce);
        VrfUtil.setDifficulty(header, emptyArray);
        VrfUtil.setMiner(header, coinbase);

        return header;
    }

    @Test
    public void testRlpEncode() {
        PrivateKey privateKey = new Ed25519PrivateKey(Hex.decode(privateKeyHex));
        VrfPrivateKey sk = new VrfPrivateKey(privateKey);

        byte[] vrfPk = sk.generatePublicKey().getEncoded();
        VrfResult vrfResult = sk.rand(seed);

        System.out.println("private key= " + Hex.toHexString(sk.getEncoded()));
        System.out.println("public key= " + Hex.toHexString(sk.generatePublicKey().getEncoded()));

        VrfProof prove1 = new VrfProof(VrfProof.ROLE_CODES_PROPOSER, 0, vrfPk, seed, vrfResult);
        byte[] rlpEncoded = prove1.getEncoded();

        VrfProof prove2 = new VrfProof(rlpEncoded);

        assertEquals(Hex.toHexString(prove1.getVrfPk()), Hex.toHexString(prove2.getVrfPk()));
        assertEquals(Hex.toHexString(prove1.getSeed()), Hex.toHexString(prove2.getSeed()));

        assertEquals(Hex.toHexString(prove1.getVrfResult().getR()), Hex.toHexString(prove2.getVrfResult().getR()));
        assertEquals(Hex.toHexString(prove1.getVrfResult().getProof()),
                Hex.toHexString(prove2.getVrfResult().getProof()));
    }

    @Test
    public void testPriority() {
        final int expected = 26;
        final int weight = 20;
        final int totalWeight = 20000;
        final int nodeCount = totalWeight / weight;

        int loop = 100;
        while (loop-- > 0) {
            int maxPriority = 0;
            int proposers = 0;
            int votes = 0;
            int weights = 0;

            int random = 0;
            for (int i = 0; i < nodeCount; ++i) {
                VrfPrivateKey sk = new VrfPrivateKey(Ed25519.getAlgorithm());

                byte[] vrfPk = sk.generatePublicKey().getEncoded();
                // Must use VrfProve Util to prove with Role Code
                VrfResult vrfResult = VrfProof.Util.prove(VrfProof.ROLE_CODES_PROPOSER, 0, sk, seed);

                VrfProof prove1 = new VrfProof(VrfProof.ROLE_CODES_PROPOSER, 0, vrfPk, seed, vrfResult);
                byte[] rlpEncoded = prove1.getEncoded();

                VrfProof prove2 = new VrfProof(rlpEncoded);

                int priority = prove2.getPriority(expected, weight, totalWeight);

                // if (priority > 0) {
                // System.out.println("priority{" + i + "}= " + priority);
                // }

                if (priority > 0) {
                    ++proposers;
                    votes += priority;
                    weights += priority * weight;

                    if (priority > maxPriority)
                        maxPriority = priority;
                }

                // assert priority < weight;
            }

            if (votes <= expected * 2 / 3) {
                System.out.println("maxPriority= " + maxPriority + ", proposers= " + proposers + ", votes= " + votes
                        + ", weights= " + weights + ", >>>>>");
            } else {
                System.out.println("maxPriority= " + maxPriority + ", proposers= " + proposers + ", votes= " + votes
                        + ", weights= " + weights);
            }

            assert proposers > 1;
            // assert weights > (totalWeight / 2);
        }
    }

    @Test
    public void testPriorityRandom1() {
        final int expected = 26;
        final int loopCount = 1000;
        final int nodeCount = 100;
        long elapseTotal = 0;

        // Assign random weight array and get total weight for VRF
        final SecureRandom secureRandom = new SecureRandom();
        final VrfPrivateKey[] skArray = new VrfPrivateKey[nodeCount];
        final int[] weightArray = new int[nodeCount];
        final int[] hitArray = new int[nodeCount];
        final Header[] blockHeaders = new Header[nodeCount];

        long totalWeight = 0;
        for (int i = 0; i < nodeCount; ++i) {
            skArray[i] = new VrfPrivateKey(Ed25519.getAlgorithm());

            if (i % 2 == 0) {
                weightArray[i] = secureRandom.nextInt(100) + 1;
            } else {
                weightArray[i] = secureRandom.nextInt(1000) + 1;
            }
            // System.out.println("weightArray[{" + i + "}= " + weightArray[i]);
            totalWeight += weightArray[i];
        }

        byte[] turnSeed = seed;

        long totalHits = 0;

        int notTarget = 0;

        int loop = loopCount;
        while (loop-- > 0) {
            int proposers = 0;
            int votes = 0;
            long elapse = 0;

            int bestPriority = 0;

            // Save it for best block header
            Header bestBlockHeader = null;

            byte[] vrfSeed = turnSeed;
            for (int i = 0; i < nodeCount; ++i) {
                // Setup block header for every node
                DataWord pubkey = DataWord.of(skArray[i].generatePublicKey().getEncoded());
                blockHeaders[i] = createBlockHeaders(loopCount - loop, pubkey.getLast20Bytes());

                byte[] vrfPk = skArray[i].generatePublicKey().getEncoded();
                // Must use VrfProve Util to prove with Role Code
                VrfResult vrfResult = VrfProof.Util.prove(VrfProof.ROLE_CODES_PROPOSER, 0, skArray[i], vrfSeed);

                VrfProof prove1 = new VrfProof(VrfProof.ROLE_CODES_PROPOSER, 0, vrfPk, vrfSeed, vrfResult);
                byte[] rlpEncoded = prove1.getEncoded();

                VrfProof prove2 = new VrfProof(rlpEncoded);

                long tmstart = System.currentTimeMillis();
                int priority = prove2.getPriority(expected, weightArray[i], totalWeight);
                long tmEnd = System.currentTimeMillis();

                long past = tmEnd - tmstart;
                elapse += past;

                // if (priority > 0) {
                // System.out.println("priority{" + i + "}= " + priority);
                // }

                if (priority > 0) {
                    totalHits += priority;
                    hitArray[i] += priority;

                    ++proposers;
                    votes += priority;

                    if (priority > bestPriority) {
                        bestPriority = priority;
                        bestBlockHeader = blockHeaders[i];

                        turnSeed = vrfResult.getR();
                    } else if (priority == bestPriority) {
                        // Check hash value, and select the small one
                        // Big Endian order
                        int compare = 0;
                        byte[] newIdentifier = blockHeaders[i].getHash().getBytes();
                        byte[] bestIdentifier = bestBlockHeader.getHash().getBytes();
                        for (int k = 0; k < newIdentifier.length; ++k) {
                            if (newIdentifier[k] < bestIdentifier[k]) {
                                compare = -1;
                                break;
                            }
                        }
                        if (compare == -1) {
                            bestPriority = priority;
                            bestBlockHeader = blockHeaders[i];

                            turnSeed = vrfResult.getR();
                        }
                    }
                }
            }

            // if (votes <= expected * 2 / 3) {
            if (votes <= expected / 2) {
                notTarget++;
                // System.out.println("<" + loop + ">, elapse= " + elapse + ", proposers= " +
                // proposers + ", votes= " + votes + ", >>>>>");
            } else {
                // System.out.println("<" + loop + ">, elapse= " + elapse + ", proposers= " +
                // proposers + ", votes= " + votes);
            }

            elapseTotal += elapse;

            assert proposers > 0;
        }

        System.out.println("totalWeight = " + totalWeight + ", notTarget = " + notTarget);

        for (int i = 0; i < nodeCount; ++i) {
            double x = (double) hitArray[i] / totalHits;
            double y = (double) weightArray[i] / totalWeight;
            double z = (x - y) / y;
            String a = String.format("%.10f", x);
            String b = String.format("%.10f", y);
            String c = String.format("%.10f", z);
            System.out.println("{" + i + "}, hit<" + hitArray[i] + ">, weight<" + weightArray[i] + ">, -- , <" + a
                    + ">, <" + b + ">, <" + c + ">");
        }

        System.out.println("Calculate Priority elapse time {" + nodeCount + "} = <"
                + elapseTotal / loopCount / nodeCount + "> ms");
    }

    @Test
    public void testPriorityRandom2() {
        final int expected = 26;
        final int loopCount = 1000;
        final int nodeCount = 100;
        long elapseTotal = 0;

        // Assign random weight array and get total weight for VRF
        final SecureRandom secureRandom = new SecureRandom();
        final VrfPrivateKey[] skArray = new VrfPrivateKey[nodeCount];
        final int[] weightArray = new int[nodeCount];
        final int[] hitArray = new int[nodeCount];
        final Header[] blockHeaders = new Header[nodeCount];

        long totalWeight = 0;
        for (int i = 0; i < nodeCount; ++i) {
            skArray[i] = new VrfPrivateKey(Ed25519.getAlgorithm());

            if (i % 2 == 0) {
                weightArray[i] = secureRandom.nextInt(100) + 1;
            } else {
                weightArray[i] = secureRandom.nextInt(1000) + 1;
            }
            // System.out.println("weightArray[{" + i + "}= " + weightArray[i]);
            totalWeight += weightArray[i];
        }

        byte[] turnSeed = seed;

        long totalHits = 0;

        int notTarget = 0;

        int loop = loopCount;
        while (loop-- > 0) {
            int proposers = 0;
            int votes = 0;
            long elapse = 0;

            int bestPriority = 0;

            // Save it for best block header
            Header bestBlockHeader = null;

            byte[] vrfSeed = turnSeed;
            for (int i = 0; i < nodeCount; ++i) {
                // Setup block header for every node
                DataWord pubkey = DataWord.of(skArray[i].generatePublicKey().getEncoded());
                blockHeaders[i] = createBlockHeaders(loopCount - loop, pubkey.getLast20Bytes());

                byte[] vrfPk = skArray[i].generatePublicKey().getEncoded();
                // Must use VrfProve Util to prove with Role Code
                VrfResult vrfResult = VrfProof.Util.prove(VrfProof.ROLE_CODES_PROPOSER, 0, skArray[i], vrfSeed);

                VrfProof prove1 = new VrfProof(VrfProof.ROLE_CODES_PROPOSER, 0, vrfPk, vrfSeed, vrfResult);
                byte[] rlpEncoded = prove1.getEncoded();

                VrfProof prove2 = new VrfProof(rlpEncoded);

                long tmstart = System.currentTimeMillis();
                int priority = prove2.getPriority(expected, weightArray[i], totalWeight);
                long tmEnd = System.currentTimeMillis();

                long past = tmEnd - tmstart;
                elapse += past;

                // if (priority > 0) {
                // System.out.println("priority{" + i + "}= " + priority);
                // }

                if (priority > 0) {
                    ++totalHits;
                    ++hitArray[i];

                    ++proposers;
                    votes += priority;

                    if (priority > bestPriority) {
                        bestPriority = priority;

                        bestBlockHeader = blockHeaders[i];

                        turnSeed = vrfResult.getR();
                    } else if (priority == bestPriority) {
                        // Check hash value, and select the small one
                        // Big Endian order
                        int compare = 0;
                        byte[] newIdentifier = blockHeaders[i].getHash().getBytes();
                        byte[] bestIdentifier = bestBlockHeader.getHash().getBytes();
                        for (int k = 0; k < newIdentifier.length; ++k) {
                            if (newIdentifier[k] < bestIdentifier[k]) {
                                compare = -1;
                                break;
                            }
                        }
                        if (compare == -1) {
                            bestPriority = priority;
                            bestBlockHeader = blockHeaders[i];

                            turnSeed = vrfResult.getR();
                        }
                    }
                }
            }

            if (votes <= expected * 2 / 3) {
                notTarget++;
                // System.out.println("<" + loop + ">, elapse= " + elapse + ", proposers= " +
                // proposers + ", votes= " + votes + ", >>>>>");
            } else {
                // System.out.println("<" + loop + ">, elapse= " + elapse + ", proposers= " +
                // proposers + ", votes= " + votes);
            }

            elapseTotal += elapse;

            assert proposers > 0;
        }

        System.out.println("totalWeight = " + totalWeight + ", notTarget = " + notTarget);

        for (int i = 0; i < nodeCount; ++i) {
            double x = (double) hitArray[i] / totalHits;
            double y = (double) weightArray[i] / totalWeight;
            double z = (x - y) / y;
            String a = String.format("%.10f", x);
            String b = String.format("%.10f", y);
            String c = String.format("%.10f", z);
            System.out.println("{" + i + "}, hit<" + hitArray[i] + ">, weight<" + weightArray[i] + ">, -- , <" + a
                    + ">, <" + b + ">, <" + c + ">");
        }

        System.out.println("Calculate Priority elapse time {" + nodeCount + "} = <"
                + elapseTotal / loopCount / nodeCount + "> ms");
    }

    @Test
    public void testPriorityRandom3() {
        final int expected = 26;
        final int loopCount = 10000;
        final int nodeCount = 1000;
        long elapseTotal = 0;

        // Assign random weight array and get total weight for VRF
        final SecureRandom secureRandom = new SecureRandom();
        final VrfPrivateKey[] skArray = new VrfPrivateKey[nodeCount];
        final int[] weightArray = new int[nodeCount];
        final int[] hitArray = new int[nodeCount];
        final Header[] blockHeaders = new Header[nodeCount];

        long totalWeight = 0;
        for (int i = 0; i < nodeCount; ++i) {
            skArray[i] = new VrfPrivateKey(Ed25519.getAlgorithm());

            // weightArray[i] = secureRandom.nextInt(1000) + 10;
            weightArray[i] = 10000;

            // System.out.println("weightArray[{" + i + "}= " + weightArray[i]);
            totalWeight += weightArray[i];
        }

        byte[] turnSeed = seed;

        long totalHits = 0;

        int bigTarget = 0;
        int notTarget = 0;
        int littleTarget = 0;

        int loop = loopCount;
        while (loop-- > 0) {
            int proposers = 0;
            int votes = 0;
            long elapse = 0;

            int bestPriority = 0;

            // Save it for best block header
            Header bestBlockHeader = null;

            byte[] vrfSeed = turnSeed;
            for (int i = 0; i < nodeCount; ++i) {
                // Setup block header for every node
                DataWord pubkey = DataWord.of(skArray[i].generatePublicKey().getEncoded());
                blockHeaders[i] = createBlockHeaders(loopCount - loop, pubkey.getLast20Bytes());

                byte[] vrfPk = skArray[i].generatePublicKey().getEncoded();
                // Must use VrfProve Util to prove with Role Code
                VrfResult vrfResult = VrfProof.Util.prove(VrfProof.ROLE_CODES_PROPOSER, 0, skArray[i], vrfSeed);

                VrfProof prove1 = new VrfProof(VrfProof.ROLE_CODES_PROPOSER, 0, vrfPk, vrfSeed, vrfResult);
                byte[] rlpEncoded = prove1.getEncoded();

                VrfProof prove2 = new VrfProof(rlpEncoded);

                long tmstart = System.currentTimeMillis();
                int priority = prove2.getPriority(expected, weightArray[i], totalWeight);
                long tmEnd = System.currentTimeMillis();

                long past = tmEnd - tmstart;
                elapse += past;

                // if (priority > 0) {
                // System.out.println("priority{" + i + "}= " + priority);
                // }

                if (priority > 0) {
                    ++totalHits;
                    ++hitArray[i];

                    ++proposers;
                    votes += priority;

                    if (priority > bestPriority) {
                        bestPriority = priority;

                        bestBlockHeader = blockHeaders[i];

                        turnSeed = vrfResult.getR();
                    } else if (priority == bestPriority) {
                        // Check hash value, and select the small one
                        // Big Endian order
                        int compare = 0;
                        byte[] newIdentifier = blockHeaders[i].getHash().getBytes();
                        byte[] bestIdentifier = bestBlockHeader.getHash().getBytes();
                        for (int k = 0; k < newIdentifier.length; ++k) {
                            if (newIdentifier[k] < bestIdentifier[k]) {
                                compare = -1;
                                break;
                            }
                        }
                        if (compare == -1) {
                            bestPriority = priority;
                            bestBlockHeader = blockHeaders[i];

                            turnSeed = vrfResult.getR();
                        }
                    }
                }
            }

            // 1/3 weights reach 2/3 votes
            if (votes > expected * 2) {
                bigTarget++;
            } else if (votes < expected / 3) {
                littleTarget++;
            }

            if (votes <= expected * 2 / 3) {
                notTarget++;
                // System.out.println("<" + loop + ">, elapse= " + elapse + ", proposers= " +
                // proposers + ", votes= " + votes + ", >>>>>");
            } else {
                // System.out.println("<" + loop + ">, elapse= " + elapse + ", proposers= " +
                // proposers + ", votes= " + votes);
            }

            elapseTotal += elapse;

            assert proposers > 0;
        }

        System.out.println("loopCount = " + loopCount + ", notTarget = " + notTarget + ", rate = "
                + (double) notTarget / loopCount);
        System.out.println("bigTarget = " + bigTarget + ", littleTarget = " + littleTarget);

        for (int i = 0; i < nodeCount; ++i) {
            double x = (double) hitArray[i] / totalHits;
            double y = (double) weightArray[i] / totalWeight;
            double z = (x - y) / y;
            String a = String.format("%.10f", x);
            String b = String.format("%.10f", y);
            String c = String.format("%.10f", z);
            System.out.println("{" + i + "}, hit<" + hitArray[i] + ">, weight<" + weightArray[i] + ">, -- , <" + a
                    + ">, <" + b + ">, <" + c + ">");
        }

        System.out.println("Calculate Priority elapse time {" + nodeCount + "} = <"
                + elapseTotal / loopCount / nodeCount + "> ms");
    }

    @Test
    public void testProposalRandom1() {
        final int expected = 26;
        final int loopCount = 1000;
        final int nodeCount = 100;
        long elapseTotal = 0;

        // Assign random weight array and get total weight for VRF
        final SecureRandom secureRandom = new SecureRandom();
        final VrfPrivateKey[] skArray = new VrfPrivateKey[nodeCount];
        final int[] weightArray = new int[nodeCount];
        final int[] hitArray = new int[nodeCount];
        final int[] proposalArray = new int[nodeCount];
        final Header[] blockHeaders = new Header[nodeCount];

        long totalWeight = 0;
        for (int i = 0; i < nodeCount; ++i) {
            skArray[i] = new VrfPrivateKey(Ed25519.getAlgorithm());

            /*
             * if (i % 2 == 0) { weightArray[i] = secureRandom.nextInt(900000) + 100000; }
             * else { weightArray[i] = secureRandom.nextInt(1000000) + 100000; }
             */

            weightArray[i] = secureRandom.nextInt(5) + 1;
            System.out.println("weightArray[{" + i + "}= " + weightArray[i]);
            totalWeight += weightArray[i];
        }

        byte[] turnSeed = seed;

        long totalHits = 0;

        int notTarget = 0;

        int loop = loopCount;
        while (loop-- > 0) {
            int proposers = 0;
            int votes = 0;
            long elapse = 0;

            int bestPriority = 0;
            int bestNode = -1;

            // Save it for best block header
            Header bestBlockHeader = null;

            byte[] vrfSeed = turnSeed;
            for (int i = 0; i < nodeCount; ++i) {
                // Setup block header for every node
                DataWord pubkey = DataWord.of(skArray[i].generatePublicKey().getEncoded());
                blockHeaders[i] = createBlockHeaders(loopCount - loop, pubkey.getLast20Bytes());

                byte[] vrfPk = skArray[i].generatePublicKey().getEncoded();
                // Must use VrfProve Util to prove with Role Code
                VrfResult vrfResult = VrfProof.Util.prove(VrfProof.ROLE_CODES_PROPOSER, 0, skArray[i], vrfSeed);

                VrfProof prove1 = new VrfProof(VrfProof.ROLE_CODES_PROPOSER, 0, vrfPk, vrfSeed, vrfResult);
                byte[] rlpEncoded = prove1.getEncoded();

                VrfProof prove2 = new VrfProof(rlpEncoded);

                long tmstart = System.currentTimeMillis();
                int priority = prove2.getPriority(expected, weightArray[i], totalWeight);
                long tmEnd = System.currentTimeMillis();

                long past = tmEnd - tmstart;
                elapse += past;

                // if (priority > 0) {
                // System.out.println("priority{" + i + "}= " + priority);
                // }

                if (priority > 0) {
                    ++totalHits;
                    ++hitArray[i];

                    ++proposers;
                    votes += priority;

                    if (priority > bestPriority) {
                        bestPriority = priority;
                        bestNode = i;

                        bestBlockHeader = blockHeaders[i];

                        turnSeed = vrfResult.getR();
                    } else if (priority == bestPriority) {
                        // Check hash value, and select the small one
                        // Big Endian order
                        int compare = 0;
                        byte[] newIdentifier = blockHeaders[i].getHash().getBytes();
                        byte[] bestIdentifier = bestBlockHeader.getHash().getBytes();
                        for (int k = 0; k < newIdentifier.length; ++k) {
                            if (newIdentifier[k] < bestIdentifier[k]) {
                                compare = -1;
                                break;
                            }
                        }
                        if (compare == -1) {
                            bestPriority = priority;
                            bestBlockHeader = blockHeaders[i];

                            turnSeed = vrfResult.getR();
                        }
                    }
                }
            }

            proposalArray[bestNode]++;

            if (votes <= expected * 2 / 3) {
                notTarget++;
                System.out.println("<" + loop + ">, elapse= " + elapse + ", best pri= " + bestPriority + ", best node= "
                        + bestNode + ", proposers= " + proposers + ", votes= " + votes + ", >>>>>");
            } else {
                System.out.println("<" + loop + ">, elapse= " + elapse + ", best pri= " + bestPriority + ", best node= "
                        + bestNode + ", proposers= " + proposers + ", votes= " + votes);
            }

            elapseTotal += elapse;

            assert proposers > 0;
        }

        System.out.println("totalWeight = " + totalWeight + ", notTarget = " + notTarget);

        for (int i = 0; i < nodeCount; ++i) {
            double p = (double) proposalArray[i] / loopCount;
            double x = (double) hitArray[i] / totalHits;
            double y = (double) weightArray[i] / totalWeight;
            double z = (x - y) / y;
            String a = String.format("%.10f", p);
            String b = String.format("%.10f", x);
            String c = String.format("%.10f", y);
            String d = String.format("%.10f", z);
            System.out.println("{" + i + "}, hit<" + hitArray[i] + ">, weight<" + weightArray[i] + ">, -- , <" + a
                    + ">, <" + b + ">, <" + c + ">, <" + d + ">");
        }

        System.out.println("Calculate Priority elapse time {" + nodeCount + "} = <"
                + elapseTotal / loopCount / nodeCount + "> ms");
    }
}
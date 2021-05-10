package org.tdf.sunflower.vrf.crypto;

import org.apache.commons.math3.distribution.BinomialDistribution;
import org.apache.commons.math3.fraction.BigFraction;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Ignore;
import org.junit.Test;
import org.tdf.crypto.ed25519.Ed25519;
import org.tdf.sunflower.consensus.vrf.struct.VrfPrivateKey;
import org.tdf.sunflower.consensus.vrf.struct.VrfPublicKey;
import org.tdf.sunflower.consensus.vrf.struct.VrfResult;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;

import static org.junit.Assert.assertTrue;

public class VrfTests {
    private static final byte[] seed = "hello world safkfkfa;fkas;f".getBytes();
    private static final int expected = 26;
    private static final int weight = 50;
    private static final int totalWeight = 1000;
    private static final int users = 100;
    private static final int rounds = 100;
    private static final Random rand = new Random();
    private static final BigInteger maxUint256 = new BigInteger(
        "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", 16);

    @Test
    public void testrand() {
        VrfPrivateKey k = new VrfPrivateKey(Ed25519.getAlgorithm());
//        VrfPrivateKey k2 = new VrfPrivateKey(Secp256k1.getAlgorithm());
        k.rand(seed);
//        k2.rand(seed);
        System.out.println("private key= " + Hex.toHexString(k.getEncoded()));
        System.out.println("public key= " + Hex.toHexString(k.generatePublicKey().getEncoded()));
    }

    @Test
    public void verifyRand() {
        VrfPrivateKey k = new VrfPrivateKey(Ed25519.getAlgorithm());
//        VrfPrivateKey k2 = new VrfPrivateKey(Secp256k1.getAlgorithm());
        VrfResult res = k.rand(seed);
//        VrfResult res2 = k2.rand(seed);
        assert k.generatePublicKey().verify(seed, res);
//        assert k2.generatePublicKey().verify(seed, res2);
    }

    @Test
    public void encodeVrfResult() {
        VrfPrivateKey k = new VrfPrivateKey(Ed25519.getAlgorithm());
//        VrfPrivateKey k2 = new VrfPrivateKey(Secp256k1.getAlgorithm());
        VrfResult res = k.rand(seed);
//        VrfResult res2 = k2.rand(seed);
        res.getEncoded();
//        res2.getEncoded();
    }

    @Test
    public void decodeVrfResult() {
        VrfPrivateKey k = new VrfPrivateKey(Ed25519.getAlgorithm());
//        VrfPrivateKey k2 = new VrfPrivateKey(Secp256k1.getAlgorithm());
        VrfResult res = k.rand(seed);
//        VrfResult res2 = k2.rand(seed);
        VrfResult n = new VrfResult(res.getEncoded());
//        VrfResult n2 = new VrfResult(res2.getEncoded());
        System.out.println(Hex.toHexString(res.getR()));
        System.out.println(Hex.toHexString(n.getR()));
        assert Arrays.equals(res.getProof(), n.getProof());
//        assert Arrays.equals(res2.getR(), n2.getR());
//        assert Arrays.equals(res2.getProof(), n2.getProof());
    }

    @Test
    public void testVrfPublicKeyEncode() {
        VrfPrivateKey k = new VrfPrivateKey(Ed25519.getAlgorithm());
        k.generatePublicKey().getEncoded();
    }

    @Test
    public void testVrfPublicKeyDecode() {
        VrfPrivateKey k = new VrfPrivateKey(Ed25519.getAlgorithm());
        byte[] encoded = k.generatePublicKey().getEncoded();
        VrfPublicKey nk = new VrfPublicKey(encoded, Ed25519.getAlgorithm());
        assert Arrays.equals(k.generatePublicKey().getEncoded(), nk.getEncoded());
    }

    @Test
    public void testCalcPriority() {
        VrfPrivateKey k = new VrfPrivateKey(Ed25519.getAlgorithm());
        VrfResult res = k.rand(seed);
//        VrfPrivateKey k2 = new VrfPrivateKey(Secp256k1.getAlgorithm());
//        VrfResult res2 = k2.rand(seed);
        int p = k.generatePublicKey().calcPriority(seed, res, expected, weight, totalWeight);
//        int p2 = k2.generatePublicKey().calcPriority(seed, res2, expected, weight, totalWeight);
        assert p < weight;
//        assert p2 < weight;
    }

    public int batchPriority(int users, int totalWeight, int weightPerUser, int expected) {
        VrfPrivateKey[] ks = new VrfPrivateKey[users];
        int[] priorities = new int[users];
        int[] weights = new int[users];
        int weight = totalWeight;
        for (int i = 0; i < users; i++) {
            ks[i] = new VrfPrivateKey(Ed25519.getAlgorithm());
            VrfResult res = ks[i].rand(seed);
            // assign weight to every user,
            int weighti = rand.nextInt(weightPerUser);
            if (weight >= weighti) {
                weights[i] = weighti;
                weight = weight - weighti;
            }
            if (i == users - 1) {
                weights[i] = weight;
            }
            priorities[i] = ks[i].generatePublicKey().calcPriority(seed, res, expected, weights[i], totalWeight);
        }
        int proposers = 0;
        for (int p : priorities) {
            if (p > 0) {
                proposers++;
            }
        }
        return proposers;
    }

    // Assume a success round contains at least one proposer,
    // this 1000 rounds test shows none fail round exists, where 100 user holds
    // approximate 10% of total weight.
    //
    @Ignore
    @Test
    public void testBatchPriority() {
        int successRounds = 0;
        for (int r = 0; r < rounds; r++) {
            int p = batchPriority(users, totalWeight, totalWeight / users, expected);
            if (p > 0) {
                successRounds++;
            }
        }
        assert successRounds == rounds;
    }

    public int batchSumPriority(int users, int totalWeight, int weightPerUser, int expected) {
        int sumPriorities = 0;
        for (int i = 0; i < users; i++) {
            VrfPrivateKey sk = new VrfPrivateKey(Ed25519.getAlgorithm());
            VrfResult res = sk.rand(seed);
            sumPriorities += sk.generatePublicKey().calcPriority(seed, res, expected, weightPerUser, totalWeight);
        }
        assert sumPriorities > 0;
        return sumPriorities;
    }

    @Ignore
    @Test
    public void testBatchSumPriority() {
        for (int r = 0; r < rounds; r++) {
            int p = batchSumPriority(users, totalWeight, totalWeight / users, expected);
            System.out.printf("%d \n", p);
        }
    }

    @Test
    public void testRoundPriority() {
        final int expected = 26;
        final int weight = 20;
        final int totalWeight = 1000;
        final int nodeCount = totalWeight / weight;

        byte[] nextSeed = seed;

        int loop = 100;
        while (loop-- > 0) {
            // begin one round select
            int proposer = 0;
            int maxPriority = 0;
            byte[] roundSeed = nextSeed;

            for (int i = 0; i < nodeCount; ++i) {
                VrfPrivateKey sk = new VrfPrivateKey(Ed25519.getAlgorithm());

                byte[] pkEncoded = sk.generatePublicKey().getEncoded();
                VrfResult vrfResult = sk.rand(roundSeed);
                VrfPublicKey pk = sk.generatePublicKey();

                int priority = pk.calcPriority(roundSeed, vrfResult, expected, weight, totalWeight);
                // System.out.println("priority= " + priority);

                if (priority > 0) {
                    ++proposer;

                    // set max priority and next seed
                    if (maxPriority < priority) {
                        maxPriority = priority;
                        nextSeed = vrfResult.getR();
                    }
                }

                assert priority < weight;
            }
            System.out.println("proposer= " + proposer);

            assert proposer >= 1;
        }
    }

    @Test
    @Ignore
    public void testCalcPriority2() {
        final int expected = 26;
        final int weight = 50;
        final int totalWeight = 20000;
        final int nodeCount = totalWeight / weight;

        int loop = 100;
        while (loop-- > 0) {
            int priorities = 0;

            for (int i = 0; i < nodeCount; ++i) {
                VrfPrivateKey sk = new VrfPrivateKey(Ed25519.getAlgorithm());

                VrfResult vrfResult = sk.rand(seed);
                VrfPublicKey pk = sk.generatePublicKey();
                priorities += pk.calcPriority(seed, vrfResult, expected, weight, totalWeight);

            }
            assert priorities >= 1;
        }
    }

    @Test
    public void testBinomial() {
        VrfPrivateKey sk = new VrfPrivateKey(Ed25519.getAlgorithm());
        VrfResult result = sk.rand(seed);
        BigFraction x = new BigFraction(new BigInteger(1, result.getR()), maxUint256);
        int w = 50;
        BinomialDistribution b = new BinomialDistribution(w, x.doubleValue());
        VrfPublicKey.Binomial b2 = new VrfPublicKey.Binomial(w, x);
        System.out.println(b.cumulativeProbability(15));
        System.out.println(b2.cumulativeProbability(15).doubleValue());
    }

    @Ignore
    @Test
    public void testCalcPriorityRandom1() {
        final int expected = 26;
        final int loopCount = 100000;
        final int nodeCount = 1;

        // Assign random weight array and get total weight for VRF
        final SecureRandom secureRandom = new SecureRandom();
        final VrfPrivateKey[] skArray = new VrfPrivateKey[nodeCount];
        final int[] weightArray = new int[nodeCount];

        long totalWeight = 0;
        for (int i = 0; i < nodeCount; ++i) {
            skArray[i] = new VrfPrivateKey(Ed25519.getAlgorithm());

            if (i % 2 == 0) {
                weightArray[i] = secureRandom.nextInt(10000) + 1;
            } else {
                weightArray[i] = secureRandom.nextInt(10000) + 1;
            }
            System.out.println("weightArray[{" + i + "}= " + weightArray[i]);
            totalWeight += weightArray[i];
        }

        byte[] turnSeed = seed;

        int maxPriority = 0;
        int minPriorities = expected;
        int maxPriorities = 0;

        int loop = loopCount;
        while (loop-- > 0) {
            int proposers = 0;
            int priorities = 0;

            int bestPriority = 0;
            byte[] vrfSeed = turnSeed;

            for (int i = 0; i < nodeCount; ++i) {
                VrfResult vrfResult = skArray[i].rand(vrfSeed);
                VrfPublicKey vrfPk = skArray[i].generatePublicKey();

                int priority = vrfPk.calcPriority(vrfSeed, vrfResult, expected, weightArray[i], totalWeight);
                if (weightArray[i] > totalWeight / 2) {
                    assertTrue(priority < expected * 2.5);
                    assertTrue(priority > 0);
                } else {
                    assertTrue(priority < expected * 1.5);
                    assertTrue(priority > 0);
                }

                if (priority > 0) {
                    proposers++;
                    priorities += priority;

                    if (priority > bestPriority) {
                        bestPriority = priority;

                        turnSeed = vrfResult.getR();
                    }

                    if (priority > maxPriority)
                        maxPriority = priority;
                }
            }

            System.out.println("<" + loop + ">, proposers = " + proposers + ", priorities = " + priorities);

            if (priorities > maxPriorities)
                maxPriorities = priorities;

            if (priorities < minPriorities)
                minPriorities = priorities;
        }

        System.out.println("maxPriority = " + maxPriority + ", maxPriorities = " + maxPriorities + ", minPriorities = "
            + minPriorities);
    }

    @Ignore
    @Test
    public void testCalcPriorityRandom2() {
        final int expected = 26;
        final int loopCount = 1000000;
        final int nodeCount = 2;

        // Assign random weight array and get total weight for VRF
        final SecureRandom secureRandom = new SecureRandom();
        final VrfPrivateKey[] skArray = new VrfPrivateKey[nodeCount];
        final int[] weightArray = new int[nodeCount];

        long totalWeight = 0;
        for (int i = 0; i < nodeCount; ++i) {
            skArray[i] = new VrfPrivateKey(Ed25519.getAlgorithm());

            if (i % 2 == 0) {
                // weightArray[i] = secureRandom.nextInt(10000) + 1;
                weightArray[i] = 101;
            } else {
                // weightArray[i] = secureRandom.nextInt(10000) + 1;
                weightArray[i] = 99;
            }
            System.out.println("weightArray[{" + i + "}= " + weightArray[i]);
            totalWeight += weightArray[i];
        }

        byte[] turnSeed = seed;

        int maxPriority = 0;
        int minPriorities = expected;
        int maxPriorities = 0;

        int loop = loopCount;
        while (loop-- > 0) {
            int proposers = 0;
            int priorities = 0;

            int bestPriority = 0;
            byte[] vrfSeed = turnSeed;

            for (int i = 0; i < nodeCount; ++i) {
                VrfResult vrfResult = skArray[i].rand(vrfSeed);
                VrfPublicKey vrfPk = skArray[i].generatePublicKey();

                int priority = vrfPk.calcPriority(vrfSeed, vrfResult, expected, weightArray[i], totalWeight);
                if (weightArray[i] > totalWeight / 2) {
                    assertTrue(priority < expected * 2.5);
                } else {
                    assertTrue(priority < expected * 1.5);
                }

                if (priority > 0) {
                    proposers++;
                    priorities += priority;

                    if (priority > bestPriority) {
                        bestPriority = priority;

                        turnSeed = vrfResult.getR();
                    }

                    if (priority > maxPriority)
                        maxPriority = priority;
                }
            }

            System.out.println("<" + loop + ">, proposers = " + proposers + ", priorities = " + priorities);

            if (priorities > maxPriorities)
                maxPriorities = priorities;

            if (priorities < minPriorities)
                minPriorities = priorities;
        }

        System.out.println("maxPriority = " + maxPriority + ", maxPriorities = " + maxPriorities + ", minPriorities = "
            + minPriorities);
    }

    @Ignore
    @Test
    public void testCalcPriorityArxm() {
        final int expected = 26;
        final int loopCount = 10000;
        final int nodeCount = 200;

        // Assign random weight array and get total weight for VRF
        final SecureRandom secureRandom = new SecureRandom();
        final VrfPrivateKey[] skArray = new VrfPrivateKey[nodeCount];
        final int[] weightArray = new int[nodeCount];

        long totalWeight = 0;
        for (int i = 0; i < nodeCount; ++i) {
            skArray[i] = new VrfPrivateKey(Ed25519.getAlgorithm());

            if (i % 2 == 0) {
                weightArray[i] = secureRandom.nextInt(1000) + 1000;
            } else {
                weightArray[i] = secureRandom.nextInt(10000) + 100000;
            }
            // System.out.println("weightArray[{" + i + "}= " + weightArray[i]);
            totalWeight += weightArray[i];
        }

        int minPriority1 = 100, minProposers1 = 100, minPriorities1 = 100;
        int maxPriority1 = 0, maxProposers1 = 0, maxPriorities1 = 0;

        int minPriority2 = 100, minProposers2 = 100, minPriorities2 = 100;
        int maxPriority2 = 0, maxProposers2 = 0, maxPriorities2 = 0;

        byte[] turnSeed1 = seed;
        byte[] turnSeed2 = seed;

        int loop = loopCount;
        while (loop-- > 0) {
            int proposers1 = 0, priorities1 = 0, bestPriority1 = 0;
            int proposers2 = 0, priorities2 = 0, bestPriority2 = 0;

            byte[] vrfSeed1 = turnSeed1;
            byte[] vrfSeed2 = turnSeed2;

            for (int i = 0; i < nodeCount; ++i) {
                VrfResult vrfResult1 = skArray[i].rand(vrfSeed1);
                VrfPublicKey vrfPk1 = skArray[i].generatePublicKey();
                int priority1 = vrfPk1.calcPriority(vrfSeed1, vrfResult1, expected, weightArray[i], totalWeight);

                VrfResult vrfResult2 = skArray[i].rand(vrfSeed2);
                VrfPublicKey vrfPk2 = skArray[i].generatePublicKey();
                int priority2 = vrfPk1.calcPriorityArxm(vrfSeed2, vrfResult2, expected, weightArray[i], totalWeight);

                System.out.println("Priority Diff = " + (priority2 - priority1));

                if (priority1 < minPriority1)
                    minPriority1 = priority1;
                if (priority1 > maxPriority1)
                    maxPriority1 = priority1;

                if (priority1 > 0) {
                    proposers1++;
                    priorities1 += priority1;

                    if (priority1 > bestPriority1) {
                        bestPriority1 = priority1;

                        turnSeed1 = vrfResult1.getR();
                    }
                }

                if (priority2 < minPriority2)
                    minPriority2 = priority2;
                if (priority2 > maxPriority2)
                    maxPriority2 = priority2;

                if (priority2 > 0) {
                    proposers2++;
                    priorities2 += priority2;

                    if (priority2 > bestPriority2) {
                        bestPriority2 = priority2;

                        turnSeed2 = vrfResult2.getR();
                    }
                }
            }

            System.out.println("Best Priority Diff = " + (bestPriority2 - bestPriority1));

            if (proposers1 < minProposers1)
                minProposers1 = proposers1;
            if (proposers1 > maxProposers1)
                maxProposers1 = proposers1;
            if (priorities1 < minPriorities1)
                minPriorities1 = priorities1;
            if (priorities1 > maxPriorities1)
                maxPriorities1 = priorities1;

            if (proposers2 < minProposers2)
                minProposers2 = proposers2;
            if (proposers2 > maxProposers2)
                maxProposers2 = proposers2;
            if (priorities2 < minPriorities2)
                minPriorities2 = priorities2;
            if (priorities2 > maxPriorities2)
                maxPriorities2 = priorities2;

            System.out.println("proposers1 = " + proposers1 + ", priorities1 = " + priorities1);
            System.out.println("proposers2 = " + proposers2 + ", priorities2 = " + priorities2);
        }

        System.out.println("minProposers1 = " + minProposers1 + ", maxProposers1 = " + maxProposers1
            + ", minPriorities1 = " + minPriorities1 + ", maxPriorities1 = " + maxPriorities1);
        System.out.println("minProposers2 = " + minProposers2 + ", maxProposers2 = " + maxProposers2
            + ", minPriorities2 = " + minPriorities2 + ", maxPriorities2 = " + maxPriorities2);
    }
}

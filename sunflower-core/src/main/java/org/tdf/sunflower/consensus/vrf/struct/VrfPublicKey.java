package org.tdf.sunflower.consensus.vrf.struct;

import org.apache.commons.math3.distribution.BinomialDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.fraction.BigFraction;
import org.tdf.crypto.CryptoException;
import org.tdf.crypto.PublicKey;
import org.tdf.crypto.ed25519.Ed25519;
import org.tdf.crypto.ed25519.Ed25519PublicKey;
import org.tdf.sunflower.consensus.vrf.HashUtil;

import java.math.BigInteger;
import java.util.Arrays;

public class VrfPublicKey {

    private static final BigInteger maxUint256 = BigInteger.valueOf(2).pow(256);
    private static final BigFraction one = new BigFraction(1);

    private PublicKey verifier;

    public VrfPublicKey(byte[] encoded, String algorithm) {
        if (algorithm.equals(Ed25519.getAlgorithm())) {
            this.verifier = new Ed25519PublicKey(encoded);
            return;
        }
//        if (algorithm.equals(Secp256k1.getAlgorithm())){
//            this.verifier = new Secp256k1PublicKey(encoded);
//            return;
//        }
        throw new CryptoException("unsupported signature policy");
    }


    public VrfPublicKey(PublicKey verifier) {
        this.verifier = verifier;
    }

    /**
     * @param result the validity of the vrf result
     * @return
     */
    public boolean verify(byte[] seed, VrfResult result) {
        if (!Arrays.equals(HashUtil.sha256(result.getProof()), result.getR())) {
            return false;
        }
        return verifier.verify(seed, result.getProof());
    }

    /**
     * @param result the validity of the vrf result
     * @return
     */
    public boolean verify(byte[] seed, int role, VrfResult result) {
        if (!Arrays.equals(HashUtil.sha256(result.getProof()), result.getR())) {
            return false;
        }

        return verifier.verify(seed, result.getProof());
    }

    public byte[] getEncoded() {
        return this.verifier.getEncoded();
    }

    /**
     * Please check Algorand paper:
     * https://github.com/DAGfans/TranStudy/blob/master/Papers/Algorand/5-CRYPTOGRAPHIC%20SORTITION.md
     *
     * @param seed        random seed
     * @param result      VRFResult
     * @param expected    expected committee size
     * @param weight      the weight of user
     * @param totalWeight sum of all user's weight
     * @return priority
     */
    public int calcPriority(byte[] seed, VrfResult result, int expected, int weight, long totalWeight) {
        if (weight > totalWeight) {
            throw new CryptoException("unsupported weight policy");
        }

        if (!this.verify(seed, result)) {
            return 0;
        }

        // Get fraction that hash/(2 ∧ hash_len)
        final double x = new BigFraction(new BigInteger(1, result.getR()), maxUint256).doubleValue();
        // Get fraction that 𝝉/w
        final BigFraction p = new BigFraction(expected, totalWeight);
        // Get B(w, p)
        final BinomialDistribution b = new BinomialDistribution(weight, p.doubleValue());

        // ∑(k=0->0) B(k: w,p)
        double lower = b.cumulativeProbability(0);
        // ∑(k=0->1) B(k: w,p)
        double upper = b.cumulativeProbability(1);

        // Get max loop index to reduce the number of cycles, we got multiple factor of expected from testing result
        int maxLoop = expected;
        if (weight > totalWeight * 3 / 4) {
            maxLoop = Math.min(weight, (int) (expected * 2.5));
        } else if (weight > totalWeight / 2) {
            maxLoop = Math.min(weight, expected * 2);
        } else if (weight > totalWeight / 4) {
            maxLoop = Math.min(weight, (int) (expected * 1.5));
        }

        int j = 0;
        while (j++ < maxLoop) {
            if (x >= lower && x < upper) {
                return j;
            }

            // ∑(k=0->j) B(k: w,p)
            lower = upper;
            // ∑(k=0->j+1) B(k: w,p)
            upper = b.cumulativeProbability(j + 1);
        }

        return 0;
    }

    public int calcPriorityArxm(byte[] seed, VrfResult result, int expected, int weight, long totalWeight) {
        if (!this.verify(seed, result)) {
            return 0;
        }

        // Get fraction that hash/(2 ∧ hash_len)
        final double x = new BigFraction(new BigInteger(1, result.getR()), maxUint256).doubleValue();

        double mean = (double) weight * expected / totalWeight;
        double sd = Math.sqrt((double) weight * expected * (totalWeight - expected) / totalWeight / totalWeight);

        final NormalDistribution normalDistribution = new NormalDistribution(mean, sd);

        // ∑(k=0->0) B(k: w,p)
        double lower = normalDistribution.cumulativeProbability(0 + 0.5);
        // ∑(k=0->1) B(k: w,p)
        double upper = normalDistribution.cumulativeProbability(1 + 0.5);

        int j = 0;
        while (j++ < weight) {
            if (x >= lower && x < upper) {
                return j;
            }

            // ∑(k=0->j) B(k: w,p)
            lower = upper;
            // ∑(k=0->j+1) B(k: w,p)
            upper = normalDistribution.cumulativeProbability(j + 1 + 0.5);
        }

        return 0;
    }

    public static class Binomial {
        private BigFraction[] pbs;
        private BigFraction[] cbs;
        private int w;
        private BigFraction p;

        public Binomial(int w, BigFraction p) {
            this.p = p;
            this.w = w;
            this.pbs = new BigFraction[w];
            this.cbs = new BigFraction[w];
        }

        private static BigFraction B(int k, int w, BigFraction p) {
            return C(w, k).multiply(p.pow(k).multiply(one.subtract(p).pow(w - k)));
        }

        private static BigFraction C(int n, int r) {
            return new BigFraction(
                    factorial(BigInteger.valueOf(n)),
                    factorial(BigInteger.valueOf(r)).multiply(factorial(BigInteger.valueOf(n - r)))
            );
        }

        private static BigInteger factorial(BigInteger n) {
            if (n.compareTo(BigInteger.ZERO) == 0) {
                return BigInteger.ONE;
            }
            return n.multiply(
                    factorial(n.subtract(BigInteger.ONE))
            );
        }

        public BigFraction prob(int k) {
            if (k < 0 || k > w) {
                return BigFraction.ZERO;
            }
            int k_w = this.w - k;
            if (k_w < k) {
                return prob(k_w);
            }
            if (pbs[k] != null) {
                return pbs[k];
            }
            BigFraction pb = B(k, this.w, this.p);
            pbs[k] = pb;
            return pbs[k];
        }

        public BigFraction cumulativeProbability(int k) {
            if (k >= this.w) {
                return BigFraction.ONE;
            }
            if (k == 0) {
                return prob(0);
            }
            if (cbs[k] != null) {
                return cbs[k];
            }
            cbs[k] = prob(k).add(
                    cumulativeProbability(k - 1)
            );
            return cbs[k];
        }
    }

}

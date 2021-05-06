package org.tdf.crypto.tutorial;

import lombok.SneakyThrows;

import java.math.BigInteger;
import java.security.MessageDigest;

public class ECDSASigner {
    private final long p;

    private final long q;

    private final long a;

    private final long b;

    private final ECC ecc;

    private final ECCPoint alpha;

    /**
     * y^2 = x^3 + ax + b (mod p)
     *
     * @param p     prime number
     * @param q     prime number, the order of point alpha
     * @param a     a
     * @param b     b
     * @param alpha point on curve, order of alpha is q, q * alpha = O (the infinity point)
     */
    public ECDSASigner(long p, long q, long a, long b, ECCPoint alpha) {
        this.p = p;
        this.q = q;
        this.a = a;
        this.b = b;
        this.ecc = new ECC(a, b, p);
        this.alpha = alpha;
        if (ecc.mul(q, alpha) != ECCPoint.INFINITY)
            throw new IllegalArgumentException("the order of " + alpha + " is not " + q);
    }

    public long[] sign(long sk, byte[] msg) {
        // k is a random integer where 1 <= k <= q-1
        long k = q - 1;
        Domain zq = new Domain(q);
        while (k >= 1) {
            ECCPoint uv = ecc.mul(k, alpha);
            long r = zq.add(uv.getX(), 0);
            if (r == 0) {
                k--;
                continue;
            }
            long kInv = zq.inverse(k);
            long s = zq.add(
                shaModQ(msg),
                zq.mul(sk, r));
            s = zq.mul(kInv, s);
            if (s == 0) {
                k--;
                continue;
            }
            return new long[]{r, s};
        }
        throw new RuntimeException("unexpected");
    }

    public ECCPoint getPublicKey(long sk) {
        return ecc.mul(sk, alpha);
    }

    public boolean verify(long[] rs, byte[] msg, ECCPoint publicKey) {
        Domain zq = new Domain(q);
        long w = zq.inverse(rs[1]);
        long i = zq.mul(w, shaModQ(msg));
        long j = zq.mul(w, rs[0]);
        ECCPoint uv = ecc.add(ecc.mul(i, alpha), ecc.mul(j, publicKey));
        return zq.add(0, uv.getX()) == rs[0];
    }

    @SneakyThrows
    private long shaModQ(byte[] msg) {
        MessageDigest sha =
            MessageDigest.getInstance("SHA");
        BigInteger b = new BigInteger(1, sha.digest(msg));
        return b.mod(BigInteger.valueOf(q)).longValue();
    }
}

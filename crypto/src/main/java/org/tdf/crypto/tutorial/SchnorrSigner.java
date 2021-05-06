package org.tdf.crypto.tutorial;

import java.util.Objects;

public class SchnorrSigner {
    private final long p;
    private final long q;
    private final long alpha;
    private final Domain d;

    /**
     * Schnorr 签名方案
     *
     * @param p     素数
     * @param q     q 要能整除 p - 1
     * @param alpha alpha ^ q = 1 (mod p)
     */
    public SchnorrSigner(long p, long q, long alpha) {
        this.p = p;
        this.q = q;
        this.d = new Domain(p);
        if (d.power(alpha, q) != 1)
            throw new IllegalArgumentException();
        this.alpha = alpha;
    }

    public long[] sign(long sk, long x) {
        long k = x % (q - 1);
        if (k == 0)
            k = 1;
        long gamma = Integer.toUnsignedLong(
            Objects.hash(x, d.power(alpha, k))
        ) % q;
        long delta = d.add(k, d.mul(sk, gamma));
        return new long[]{gamma, delta};
    }

    public boolean verify(long[] sig, long x, long beta) {
        return sig[0]
            ==
            Integer.toUnsignedLong(Objects.hash(
                x,
                d.mul(
                    d.power(alpha, sig[1]),
                    d.inverse(d.power(beta, sig[0]))
                )
            )) % q;
    }

    public long getBeta(long a) {
        return d.power(alpha, a);
    }
}

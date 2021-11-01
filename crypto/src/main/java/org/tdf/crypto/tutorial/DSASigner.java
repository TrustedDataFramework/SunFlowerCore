package org.tdf.crypto.tutorial;

import lombok.SneakyThrows;

import java.math.BigInteger;
import java.security.MessageDigest;

public class DSASigner {
    private final long p;
    private final long q;
    private final long alpha;
    private final Domain zp;

    /**
     * @param p     长 L bit 的素数 512 <= L <= 1024
     * @param q     素数 160bit  且 (p - 1) % q = 0
     * @param alpha alpha ^ q = 1 (mod p)
     */
    public DSASigner(long p, long q, long alpha) {
        this.p = p;
        this.q = q;
        if ((p - 1) % q != 0)
            throw new RuntimeException("p - 1 % q != 0, p = " + p + " q = " + q);
        this.zp = new Domain(p);
        this.alpha = alpha;
        assert zp.power(alpha, q) == 1;
    }


    /**
     * @param a 私钥 0 <= a <= q - 1
     * @return 公钥
     */
    public long getBeta(long a) {
        return zp.power(alpha, a);
    }

    /**
     * @param sk 私钥 0 <= sk <= q - 1
     * @return 公钥
     */
    @SneakyThrows
    public long[] sign(long sk, byte[] msg) {
        Domain zq = new Domain(q);
        // 获取伪随机的 k
        long k = 0;
        long kInv = 0;
        for (long i = zq.getP() - 1; i > 0; i--) {
            try {
                k = zq.inverse(i);
                kInv = i;
                break;
            } catch (Exception ignored) {

            }
        }
        if (k == 0)
            throw new RuntimeException("cannot find k at z* " + zq.getP());

        long gamma = zp.power(alpha, k);
        gamma = zq.add(gamma, 0);
        long delta = shaModQ(msg);
        delta = zq.add(delta, zq.mul(sk, gamma));
        delta = zq.mul(delta, zq.inverse(k));
        return new long[]{gamma, delta};
    }

    @SneakyThrows
    private long shaModQ(byte[] msg) {
        MessageDigest sha =
            MessageDigest.getInstance("SHA");
        BigInteger b = new BigInteger(1, sha.digest(msg));
        return b.mod(BigInteger.valueOf(q)).longValue();
    }

    public boolean verify(long[] sig, byte[] msg, long beta) {
        Domain zq = new Domain(q);
        long gamma = sig[0];
        long delta = sig[1];
        long e1 = zq.mul(
            shaModQ(msg),
            zq.inverse(delta)
        );
        long e2 = zq.mul(gamma, zq.inverse(delta));
        long r = zp.mul(
            zp.power(alpha, e1),
            zp.power(beta, e2)
        );
        return zq.add(r, 0) == gamma;
    }
}

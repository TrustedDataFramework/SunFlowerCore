package org.tdf.crypto.tutorial;

public class ElgamalSigner {
    private final long alpha;
    private final long p;
    private final Domain d;

    public ElgamalSigner(long alpha, long p) {
        this.alpha = alpha;
        this.p = p;
        this.d = new Domain(p);
    }

    /**
     * 从私钥生成公钥
     *
     * @param a 私钥
     * @return 公钥
     */
    public long getBeta(long a) {
        return this.d.power(alpha, a);
    }

    /**
     * elgamal 签名方案
     *
     * @param sk 私钥
     * @param x  签名原文
     * @return 签名
     */
    public long[] sign(long sk, long x) {
        Domain zp_1 = new Domain(p - 1);
        // 获取伪随机的 k
        long k = 0;
        long kInv = 0;
        for (long i = zp_1.getP() - 1; i > 0; i--) {
            try {
                k = zp_1.inverse(i);
                kInv = i;
                break;
            } catch (Exception ignored) {

            }
        }
        if (k == 0)
            throw new RuntimeException("cannot find k at z* " + zp_1.getP());

        long gamma = d.power(alpha, k);
        long delta = zp_1.sub(x, zp_1.mul(sk, gamma));
        delta = zp_1.mul(delta, kInv);
        return new long[]{gamma, delta};
    }

    /**
     * elgamal 签名验证
     *
     * @param sig  签名
     * @param x    签名原文
     * @param beta 公钥 beta = power(alpha, sk)
     * @return
     */
    public boolean verify(long[] sig, long x, long beta) {
        long gamma = sig[0];
        long delta = sig[1];
        return d.mul(
                d.power(beta, gamma),
                d.power(gamma, delta)
        ) == d.power(alpha, x);
    }
}

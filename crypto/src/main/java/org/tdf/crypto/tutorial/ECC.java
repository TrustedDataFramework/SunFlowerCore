package org.tdf.crypto.tutorial;

import lombok.Getter;

/**
 * 椭圆曲线
 * y^2 = x^3 + ax + b (mod p)
 */
@Getter
public class ECC {
    private final long a;
    private final long b;
    private final long p;
    private final Domain domain;
    private final long size;

    public ECC(long a, long b, long p) {
        this.a = a;
        this.b = b;
        this.p = p;
        this.domain = new Domain(p);
        long s = 0;
        for (long x = 0; x < p; x++) {
            for (long y = 0; y < p; y++) {
                if (isOnCurve(x, y))
                    s++;
            }
        }
        this.size = s + 1;
    }

    public static void main(String[] args) {
        ECC ecc = new ECC(1, 6, 11);
        ECCPoint alpha = new ECCPoint(2, 7);
        long sk = 7;
        ECCPoint pk = ecc.mul(sk, alpha);
        ECCPoint plain = new ECCPoint(10, 9);
        long k = 3;
        ECCPoint y1 = ecc.mul(k, alpha);
        ECCPoint y2 = ecc.add(plain, ecc.mul(k, pk));
        ECCPoint d = ecc.mul(sk, y1);
        d = ecc.negative(d);
        d = ecc.add(y2, d);
        System.out.println(d);
    }

    static void assertTrue(boolean b) {
        if (!b)
            throw new AssertionError();
    }

    public boolean isOnCurve(ECCPoint point) {
        return isOnCurve(point.getX(), point.getY());
    }

    public boolean isOnCurve(long x, long y) {
        long r = domain.power(x, 3);
        r = domain.add(r, domain.mul(x, a));
        r = domain.add(r, b);
        long l = domain.power(y, 2);
        return l == r;
    }

    public ECCPoint mul(long a, ECCPoint alpha) {
        if (a == 0)
            return ECCPoint.INFINITY;
        ECCPoint ret = alpha;
        for (int i = 1; i < a; i++) {
            ret = add(ret, alpha);
        }
        return ret;
    }

    public ECCPoint negative(ECCPoint point) {
        return new ECCPoint(point.getX(), domain.negative(point.getY()));
    }

    public ECCPoint add(ECCPoint pointP, ECCPoint pointQ) {
        if (pointP == ECCPoint.INFINITY)
            return pointQ;
        if (pointQ == ECCPoint.INFINITY)
            return pointP;
        long x1 = pointP.getX();
        long y1 = pointP.getY();
        long x2 = pointQ.getX();
        long y2 = pointQ.getY();
        if (x1 == x2 && domain.negative(y2) == y1)
            return ECCPoint.INFINITY;
        final long lambda;
        if (!pointP.equals(pointQ)) {
            long r = domain.sub(y2, y1);
            long inv = domain.sub(x2, x1);
            inv = domain.inverse(inv);
            lambda = domain.mul(r, inv);
        } else {
            long r = domain.power(x1, 2);
            r = domain.mul(3, r);
            r = domain.add(r, a);
            long inv = domain.mul(2, y1);
            inv = domain.inverse(inv);
            lambda = domain.mul(r, inv);
        }
        long x3 = domain.power(lambda, 2);
        x3 = domain.sub(x3, x1);
        x3 = domain.sub(x3, x2);
        long y3 = domain.mul(lambda, domain.sub(x1, x3));
        y3 = domain.sub(y3, y1);
        return new ECCPoint(x3, y3);
    }
}

package org.tdf.crypto.tutorial;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class Domain {
    private final long p;

    public long mul(long x, long y) {
        return (x * y) % p;
    }

    public long add(long x, long y) {
        return (x + y) % p;
    }

    public long sub(long x, long y) {
        return add(x, negative(y));
    }

    public long negative(long x) {
        long r = p - x;
        while (r < 0)
            r += p;
        return r % p;
    }

    public long power(long m, long n) {
        if (n < 0)
            throw new IllegalArgumentException(n + " < 0");
        if (n == 0)
            return 1;
        long ret = 1;
        for (long i = 0; i < n; i++) {
            ret = mul(ret, m);
        }
        return ret;
    }

    public long inverse(long a) {
        for (long i = 1; i < p; i++) {
            if (mul(a, i) == 1)
                return i;
        }
        throw new IllegalArgumentException();
    }

    /**
     * 获取 模 p 下 a 的阶
     *
     * @param a
     * @return
     */
    public long getN(long a) {
        for (int i = 1; i < p; i++) {
            if (power(a, i) == 1)
                return i;
        }
        return -1;
    }
}

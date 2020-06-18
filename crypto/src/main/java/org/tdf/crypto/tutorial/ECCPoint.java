package org.tdf.crypto.tutorial;

import lombok.Value;

@Value
public class ECCPoint {
    static final ECCPoint INFINITY = new ECCPoint(Long.MAX_VALUE, Long.MAX_VALUE);
    long x;
    long y;
}

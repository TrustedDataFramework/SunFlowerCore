package org.tdf.sunflower.state;

public class SafeMath {
    public static long add(long x, long y){
        if(x < 0 || y < 0)
            throw new RuntimeException("math overflow");
        long z = x + y;
        if(z < x || z < y)
            throw new RuntimeException("math overflow");
        return z;
    }

    public static long sub(long x, long y){
        if(x < 0 || y < 0)
            throw new RuntimeException("math overflow");
        if(y > x)
            throw new RuntimeException("math overflow");
        return x - y;
    }
}

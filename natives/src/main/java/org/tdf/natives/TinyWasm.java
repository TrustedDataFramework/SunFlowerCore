package org.tdf.natives;

public class TinyWasm {
    public static native int loadModule(byte[] data);
    public static void onInterrupt(int d, byte[] memory, int[] operations, String method, long[] args) {

    }
}

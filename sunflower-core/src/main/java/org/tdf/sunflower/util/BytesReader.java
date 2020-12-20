package org.tdf.sunflower.util;

import java.util.Arrays;

public class BytesReader {
    private final byte[] data;
    private int pc;

    public BytesReader(byte[] data) {
        this.data = data;
    }

    public byte read() {
        byte b = data[pc];
        pc++;
        return b;
    }

    public byte[] read(int size) {
        if (pc + size > data.length) return new byte[0];
        byte[] res = Arrays.copyOfRange(data, pc, pc + size);
        pc += size;
        return res;
    }

    public byte[] readAll() {
        if (pc >= data.length) return new byte[0];
        byte[] res = Arrays.copyOfRange(data, pc, data.length);
        pc = data.length;
        return res;
    }
}

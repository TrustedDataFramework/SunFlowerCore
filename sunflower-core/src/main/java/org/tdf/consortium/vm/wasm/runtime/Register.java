package org.wisdom.consortium.vm.wasm.runtime;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.stream.Collectors;

public class Register {
    public static final int DEFAULT_INITIAL_STACK_CAP = 16;

    private long[] data;
    private int pc;
    private LinkedList<Integer> startPCs = new LinkedList<>();

    public Register(long[] data) {
        this.data = data;
        this.pc = data.length;
    }

    public Register() {
        this(DEFAULT_INITIAL_STACK_CAP);
    }

    public void pushLabel() {
        startPCs.push(pc);
    }

    public void popLabel() {
        startPCs.pop();
    }

    public void popAndClearLabel() {
        this.pc = startPCs.pop();
    }

    public Register(int initialSize) {
        this.data = new long[initialSize];
    }

    public int size() {
        return pc;
    }

    public int cap() {
        return data.length;
    }

    private void grow() {
        if (pc >= data.length) {
            long[] tmp = data;
            data = new long[tmp.length * 2 + 1];
            System.arraycopy(tmp, 0, data, 0, tmp.length);
        }
    }


    public long[] popN(int n) {
        if (n == 0) return new long[0];
        long[] res = Arrays.copyOfRange(data, pc - n, pc);
        pc -= n;
        return res;
    }

    public long[] popAll() {
        long[] all = Arrays.copyOfRange(data, 0, pc);
        pc = 0;
        return all;
    }

    public void pushAll(long[] all) {
        for (long l : all) {
            push(l);
        }
    }

    public long pop() {
        return data[--pc];
    }

    public byte popI8() {
        return (byte) pop();
    }

    public short popI16() {
        return (short) pop();
    }

    public int popI32() {
        return (int) popI64();
    }

    public int popU32() {
        int i = popI32();
        if (i < 0) throw new RuntimeException("the uint exceed integers maximum value");
        return i;
    }

    public long popI64() {
        return data[--pc];
    }

    public float popF32() {
        return Float.intBitsToFloat(popI32());
    }

    public double popF64() {
        return Double.longBitsToDouble(popI64());
    }

    public void push(long i) {
        grow();
        data[pc++] = i;
    }

    public void pushI8(byte b) {
        push(Byte.toUnsignedLong(b));
    }

    public void pushI16(short s) {
        push(Short.toUnsignedLong(s));
    }

    public void pushI32(int i) {
        pushI64(Integer.toUnsignedLong(i));
    }

    public void pushI64(long i) {
        push(i);
    }

    public void pushF32(float i) {
        pushI32(Float.floatToIntBits(i));
    }

    public void pushF64(double i) {
        pushI64(Double.doubleToLongBits(i));
    }


    public void pushBoolean(boolean b) {
        if (b) {
            push(1);
            return;
        }
        push(0);
    }

    public long get(int index) {
        return data[index];
    }

    public void set(int index, long value) {
        data[index] = value;
    }

    public long getI64(int index) {
        return get(index);
    }

    public int getI32(int index) {
        return (int) get(index);
    }

    public int getU32(int index){
        int i = getI32(index);
        if (i < 0) throw new RuntimeException("the uint exceed integers maximum value");
        return i;
    }


    public float getF32(int index) {
        return Float.intBitsToFloat(getI32(index));
    }

    public double getF64(int index) {
        return Double.longBitsToDouble(get(index));
    }

    public void setI64(int index, long n) {
        set(index, n);
    }

    public void setI32(int index, int n) {
        set(index, Integer.toUnsignedLong(n));
    }

    public void setF32(int index, float n) {
        setI32(index, Float.floatToIntBits(n));
    }

    public void setF64(int index, double n) {
        set(index, Double.doubleToLongBits(n));
    }

    public int current() {
        return pc;
    }

    public long[] getData() {
        return data;
    }

    @Override
    public String toString() {
        return Arrays.stream(Arrays.copyOfRange(data, 0, pc))
                .mapToObj(x -> "[" + x + "]").collect(Collectors.joining());
    }

    // set pc to maximum
    public void fillAll() {
        pc = data.length;
    }
}

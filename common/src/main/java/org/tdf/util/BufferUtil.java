package org.tdf.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * util for serialize & deserialize
 */
public class BufferUtil {
    private ByteBuffer byteBuffer;
    private boolean readOnly;
    private boolean writeOnly;
    private ByteArrayOutputStream outputStream;

    public static BufferUtil newReadOnly(byte[] data){
        return new BufferUtil(data);
    }

    public static BufferUtil newWriteOnly(){
        return new BufferUtil();
    }

    private BufferUtil(byte[] data) {
        this.byteBuffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        this.readOnly = true;
    }

    private BufferUtil() {
        outputStream = new ByteArrayOutputStream();
        this.writeOnly = true;
    }

    private void requireRead() {
        if (writeOnly) throw new RuntimeException("read from write only buffer");
    }

    private void requireWrite() {
        if (readOnly) throw new RuntimeException("write to read only buffer");
    }

    public void putString(String s) {
        requireWrite();
        putBytes(s.getBytes(StandardCharsets.UTF_8));
    }

    public String getString() {
        requireRead();
        return new String(getBytes(), StandardCharsets.UTF_8);
    }

    public byte[] getBytes() {
        requireRead();
        int len = byteBuffer.getInt();
        byte[] data = new byte[len];
        byteBuffer.get(data);
        return data;
    }

    public void putBytes(byte[] data) {
        requireWrite();
        try {
            putInt(data.length);
            outputStream.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getInt() {
        requireRead();
        return byteBuffer.getInt();
    }

    public void putInt(int n) {
        requireWrite();
        try {
            outputStream.write(LittleEndian.encodeInt32(n));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void putLong(long n) {
        requireWrite();
        try {
            outputStream.write(LittleEndian.encodeInt64(n));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public byte[] getRemained(){
        byte[] buf = new byte[byteBuffer.remaining()];
        byteBuffer.get(buf);
        return buf;
    }

    public byte[] toByteArray() {
        requireWrite();
        byte[] all = outputStream.toByteArray();
        outputStream = new ByteArrayOutputStream();
        return all;
    }

    public void putLongs(long[] longs) {
        requireWrite();
        putInt(longs.length);
        for (long n : longs) {
            putLong(n);
        }
    }

    public long getLong(){
        requireRead();
        return byteBuffer.getLong();
    }

    public long[] getLongs(){
        requireRead();
        int len = getInt();
        long[] res = new long[len];
        for(int i = 0; i < res.length; i++){
            res[i] = getLong();
        }
        return res;
    }

    public int remaining() {
        requireRead();
        return byteBuffer.remaining();
    }
}

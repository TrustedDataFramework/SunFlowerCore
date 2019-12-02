package org.wisdom.consortium.vm.wasm;

import org.wisdom.util.BigEndian;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Constants {
    public static final int MAGIC_NUMBER = ByteBuffer
            .wrap(new byte[]{0x00, 0x61, 0x73, 0x6d})
            .order(ByteOrder.LITTLE_ENDIAN).getInt();

    public static final int VERSION = ByteBuffer
            .wrap(new byte[]{0x01, 0x00, 0x00, 0x00})
            .order(ByteOrder.LITTLE_ENDIAN).getInt();

    public static final int RESULT_EMPTY = 0x40;

    public static final int VALUE_I32 = 0x7f;

    public static final int VALUE_I64 = 0x7e;

    public static final int VALUE_F32 = 0x7d;

    public static final int VALUE_F64 = 0x7c;

    public static final long MEMORY_GROW_GAS_USAGE = 16 * 1024;

    public static void main(String[] args) {
        // asm
        System.out.println(new String(BigEndian.encodeInt32(MAGIC_NUMBER)));
    }
}

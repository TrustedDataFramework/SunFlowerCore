package org.tdf.sunflower.vm.hosts;

import org.tdf.common.util.BigEndian;
import org.tdf.evm.MutableBigInteger;
import org.tdf.evm.SlotUtils;
import org.tdf.lotusvm.runtime.HostFunction;
import org.tdf.lotusvm.types.FunctionType;
import org.tdf.lotusvm.types.ValueType;
import org.tdf.sunflower.vm.abi.WbiType;

import java.util.Arrays;
import java.util.Collections;


import static org.tdf.evm.SlotUtils.*;
import static org.tdf.sunflower.vm.WBI.*;

public class U256Host extends HostFunction {
    private static final int[] ZERO_SLOT = new int[SLOT_SIZE * 2];

    private final int[] slot0 = new int[SLOT_SIZE];
    private final int[] slot1 = new int[SLOT_SIZE];
    private final int[] varSlot0 = new int[SLOT_SIZE * 2];
    private final int[] varSlot1 = new int[SLOT_SIZE * 2];

    private final int[] divisor = new int[SLOT_SIZE * 2];


    private final byte[] tempBytes = new byte[MAX_BYTE_ARRAY_SIZE];

    private final MutableBigInteger mut0 = new MutableBigInteger(slot0);
    private final MutableBigInteger mut1 = new MutableBigInteger(slot1);

    private final MutableBigInteger mut2 = new MutableBigInteger(varSlot0);
    private final MutableBigInteger mut3 = new MutableBigInteger(varSlot1);


    enum U256OP {
        ADD,
        SUB,
        MUL,
        DIV,
        MOD
    }

    public static final FunctionType FUNCTION_TYPE = new FunctionType(
        // offset, length, offset
        Arrays.asList(ValueType.I64, ValueType.I64, ValueType.I64),
        Collections.singletonList(ValueType.I64));

    public U256Host() {
        super("_u256", FUNCTION_TYPE);
    }

    // override slot by uint256 pointer
    private void copyToSlot(int[] slot, long pointer) {
        // call peek function, convert uint256 pointer to byte array fat pointer
        long startAndLen = instance.execute(WBI_PEEK, pointer, WbiType.UINT_256)[0];

        int offset = (int) (startAndLen >>> 32);
        int len = (int) (startAndLen & 0xffffffffL);
        if (len > MAX_BYTE_ARRAY_SIZE)
            throw new RuntimeException("invalid uint256, overflow");

        // reset temp
        Arrays.fill(tempBytes, (byte) 0);

        // read memory referred by fat pointer into temp byte array
        getMemory().read(offset, tempBytes, MAX_BYTE_ARRAY_SIZE - len, len);
        // decode temp byte array into slot
        SlotUtils.decodeBE(tempBytes, 0, slot, 0);
    }

    // write slot into memory
    private long writeSlot(int[] slot) {
        if (slot == ZERO_SLOT) {
            long ptr = instance.execute(WBI_MALLOC, 0)[0];
            long p = instance.execute(WBI_CHANGE_TYPE, WbiType.UINT_256, ptr, 0)[0];
            int r = (int) p;
            if (r < 0) throw new RuntimeException("malloc failed: pointer is negative");
            return p;
        }

        // reset temp byte array
        Arrays.fill(tempBytes, (byte) 0);

        // encode into temp byte array
        for (int i = 0; i < SLOT_SIZE; i++) {
            if (slot[i] != 0)
                BigEndian.encodeInt32(slot[i], tempBytes, i * INT_SIZE);
        }

        // size of tempBytes without leading zeros
        int effectiveSize = 0;

        for (int i = 0; i < MAX_BYTE_ARRAY_SIZE; i++) {
            if (tempBytes[i] != 0) {
                effectiveSize = MAX_BYTE_ARRAY_SIZE - i;
                break;
            }
        }

        // call malloc, allocate a memory, get the fat pointer
        long ptr = instance.execute(WBI_MALLOC, effectiveSize)[0];
        // override the allocated memory by uint256 data
        instance.getMemory().write((int) ptr, tempBytes, MAX_BYTE_ARRAY_SIZE - effectiveSize, effectiveSize);
        // call change_t function, convert fat pointer to uint256 pointer
        long p = instance.execute(WBI_CHANGE_TYPE, WbiType.UINT_256, ptr, effectiveSize)[0];
        int r = (int) p;
        if (r < 0) throw new RuntimeException("malloc failed: pointer is negative");
        return p;
    }

    @Override
    public long execute(long[] longs) {
        int i = (int) longs[0];
        U256OP op = U256OP.values()[i];

        copyToSlot(slot0, longs[1]);
        copyToSlot(slot1, longs[2]);

        if (op == U256OP.MUL || op == U256OP.DIV || op == U256OP.MOD) {
            mut0.setValue(slot0, SLOT_SIZE);
            mut0.normalize();
            mut1.setValue(slot1, SLOT_SIZE);
            mut1.normalize();
            mut2.clear();
        }


        switch (op) {
            case ADD: {
                SlotUtils.add(slot0, 0, slot1, 0, slot0, 0);
                return writeSlot(slot0);
            }
            case SUB: {
                SlotUtils.subMut(slot0, 0, slot1, 0, slot0, 0);
                return writeSlot(slot0);
            }
            case MUL: {
                if (mut0.isZero() || mut1.isZero()) {
                    return writeSlot(ZERO_SLOT);
                }
                mut0.multiply(mut1, mut2);
                // keep at most 256 least significant bits
                Arrays.fill(slot0, 0);
                SlotUtils.trim256(varSlot0, mut2.getOffset(), mut2.getIntLen(), slot0, 0);
                return writeSlot(slot0);
            }
            case DIV: {
                // reset temporary divisor and rem
                Arrays.fill(divisor, 0);
                mut3.clear();

                mut0.divideKnuth(mut1, mut2, mut3, divisor, false);

                Arrays.fill(slot0, 0);
                SlotUtils.trim256(mut2.getValue(), mut2.getOffset(), mut2.getIntLen(), slot0, 0);
                return writeSlot(slot0);
            }
            case MOD: {
                // reset temporary divisor and rem
                Arrays.fill(divisor, 0);
                mut3.clear();

                mut0.divideKnuth(mut1, mut2, mut3, divisor, true);

                Arrays.fill(slot0, 0);
                SlotUtils.trim256(mut3.getValue(), mut3.getOffset(), mut3.getIntLen(), slot0, 0);
                return writeSlot(slot0);
            }
            default:
                throw new UnsupportedOperationException();
        }
    }
}

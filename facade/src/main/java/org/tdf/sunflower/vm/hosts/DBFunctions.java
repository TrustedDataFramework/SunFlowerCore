package org.tdf.sunflower.vm.hosts;

import lombok.Getter;
import org.tdf.common.util.HexBytes;
import org.tdf.lotusvm.runtime.HostFunction;
import org.tdf.lotusvm.types.FunctionType;
import org.tdf.lotusvm.types.ValueType;
import org.tdf.sunflower.vm.Backend;
import org.tdf.sunflower.vm.WBI;
import org.tdf.sunflower.vm.abi.WbiType;

import java.util.Arrays;
import java.util.Collections;

public class DBFunctions extends HostFunction {
    @Getter
    private final Backend backend;
    private final HexBytes address;

    public static final FunctionType FUNCTION_TYPE = new FunctionType(
            Arrays.asList(ValueType.I64, ValueType.I64, ValueType.I64),
            Collections.singletonList(ValueType.I64)
    );

    public DBFunctions(Backend backend, HexBytes address) {
        super("_db", FUNCTION_TYPE);
        this.backend = backend;
        this.address = address;
    }

    private HexBytes getKey(long... longs) {
        return (HexBytes) WBI
                .peek(getInstance(), (int) longs[1], WbiType.BYTES);
    }

    private HexBytes getValue(long... longs) {
        return (HexBytes) WBI
                .peek(getInstance(), (int) longs[2], WbiType.BYTES);
    }

    @Override
    public long execute(long... longs) {
        Type t = Type.values()[(int) longs[0]];
        switch (t) {
            case SET: {
                HexBytes key = getKey(longs);
                HexBytes value = getValue(longs);
                this.backend.dbSet(address, key, value);
                return 0;
            }
            case GET: {
                HexBytes key = getKey(longs);
                HexBytes value = this.backend.dbGet(address, key);
                return WBI
                        .mallocBytes(getInstance(), value);
            }
            case HAS: {
                HexBytes key = getKey(longs);
                return backend.dbHas(address, key) ? 1 : 0;
            }
            case REMOVE: {
                HexBytes key = getKey(longs);
                backend.dbRemove(address, key);
                return 0;
            }
//            case NEXT: {
//                this.index++;
//                break;
//            }
//            case HAS_NEXT: {
//                return new long[]{this.index < entries.size() - 1 ? 1 : 0};
//            }
//            case CURRENT_KEY: {
//                Map.Entry<HexBytes, byte[]> entry = entries.get(index);
//                if (longs[2] != 0) {
//                    putMemory((int) longs[1], entry.getKey().getBytes());
//                }
//                return new long[]{entry.getKey().size()};
//            }
//            case CURRENT_VALUE: {
//                Map.Entry<HexBytes, byte[]> entry = entries.get(index);
//                if (longs[2] != 0) {
//                    putMemory((int) longs[1], entry.getValue());
//                }
//                return new long[]{entry.getValue().length};
//            }
//            case RESET: {
//                reset();
//                break;
//            }
            default:
                throw new RuntimeException("unreachable");
        }
//        return new long[1];
    }

    enum Type {
        SET, GET, REMOVE, HAS,
    }
}

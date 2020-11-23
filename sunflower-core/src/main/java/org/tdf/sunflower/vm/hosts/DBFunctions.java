package org.tdf.sunflower.vm.hosts;

import lombok.Getter;
import org.tdf.common.trie.Trie;
import org.tdf.common.util.HexBytes;
import org.tdf.lotusvm.runtime.HostFunction;
import org.tdf.lotusvm.types.FunctionType;
import org.tdf.lotusvm.types.ValueType;
import org.tdf.sunflower.vm.abi.AbiDataType;

import java.util.*;

public class DBFunctions extends HostFunction {
    @Getter
    private final Trie<byte[], byte[]> storageTrie;
    private final boolean readonly;
    private List<Map.Entry<HexBytes, byte[]>> entries;
    private int index;
    public DBFunctions(Trie<byte[], byte[]> storageTrie, boolean readonly) {
        this.storageTrie = storageTrie;
        setName("_db");
        setType(new FunctionType(
                Arrays.asList(ValueType.I64, ValueType.I64, ValueType.I64),
                Collections.singletonList(ValueType.I64)
        ));
        reset();
        this.readonly = readonly;
    }

    private void reset() {
        Map<HexBytes, byte[]> m = new TreeMap<>();
        storageTrie.forEach((x, y) -> m.put(HexBytes.fromBytes(x), y));
        this.entries = new ArrayList<>(m.entrySet());
    }

    private void assertReadOnly(Type t) {
        switch (t) {
            case SET:
            case REMOVE:
                if (readonly)
                    throw new RuntimeException("readonly");
                break;
        }

    }

    private byte[] getKey(long... longs){
        return (byte[]) WBI
                .peek(getInstance(), (int) longs[1], AbiDataType.BYTES);
    }

    private byte[] getValue(long... longs){
        return (byte[]) WBI
                .peek(getInstance(), (int) longs[2], AbiDataType.BYTES);
    }

    @Override
    public long[] execute(long... longs) {
        Type t = Type.values()[(int) longs[0]];
        assertReadOnly(t);
        switch (t) {
            case SET: {
                byte[] key = getKey(longs);
                byte[] value = getValue(longs);
                this.storageTrie.put(key, value);
                return new long[1];
            }
            case GET: {
                byte[] key = getKey(longs);
                byte[] value = storageTrie.get(key).orElseThrow(() -> new RuntimeException(HexBytes.fromBytes(key) + " not found"));
                long r = WBI
                        .mallocBytes(getInstance(), value);
                return new long[]{r};
            }
            case HAS: {
                byte[] key = getKey(longs);
                return new long[]{storageTrie.containsKey(key) ? 1 : 0};
            }
            case REMOVE: {
                if (readonly)
                    throw new RuntimeException("readonly");
                byte[] key = getKey(longs);
                storageTrie.remove(key);
                return new long[1];
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
        SET, GET, REMOVE, HAS, NEXT, CURRENT_KEY, CURRENT_VALUE, HAS_NEXT, RESET
    }
}

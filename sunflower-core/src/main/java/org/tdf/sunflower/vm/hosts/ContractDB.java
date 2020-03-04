package org.tdf.sunflower.vm.hosts;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.tdf.common.trie.Trie;
import org.tdf.lotusvm.runtime.HostFunction;
import org.tdf.lotusvm.types.FunctionType;
import org.tdf.lotusvm.types.ValueType;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
public class ContractDB {
    @Getter
    private final Trie<byte[], byte[]> storageTrie;

    public List<HostFunction> getHelpers(){
        return Arrays.asList(
                new DBGet(storageTrie),
                new DBGetLen(storageTrie),
                new DBSet(storageTrie),
                new DBHas(storageTrie)
        );
    }

    public static class DBGet extends HostFunction{
        private final Trie<byte[], byte[]> storageTrie;

        public DBGet(Trie<byte[], byte[]> storageTrie) {
            this.storageTrie = storageTrie;
            setName("_db_get");
            setType(new FunctionType(
                    Arrays.asList(ValueType.I32, ValueType.I32, ValueType.I32),
                    Collections.emptyList()
            ));
        }

        @Override
        public long[] execute(long... longs) {
            byte[] key = loadMemory((int)longs[0], (int) longs[1]);
            byte[] val = storageTrie.get(key).orElseThrow(
                    () -> new RuntimeException("execute contract failed, key not exists in db")
            );
            putMemory((int)longs[2], val);
            return new long[0];
        }
    }

    public static class DBGetLen extends HostFunction{
        private final Trie<byte[], byte[]> storageTrie;

        public DBGetLen(Trie<byte[], byte[]> storageTrie) {
            this.storageTrie = storageTrie;
            setName("_db_get_len");
            setType(new FunctionType(
                    Arrays.asList(ValueType.I32, ValueType.I32),
                    Collections.singletonList(ValueType.I32)
            ));
        }

        @Override
        public long[] execute(long... longs) {
            byte[] key = loadMemory((int)longs[0], (int) longs[1]);
            byte[] val = storageTrie.get(key).orElseThrow(
                    () -> new RuntimeException("execute contract failed, key not exists in db")
            );
            return new long[]{val.length};
        }
    }

    public static class DBSet extends HostFunction{
        private final Trie<byte[], byte[]> storageTrie;

        public DBSet(Trie<byte[], byte[]> storageTrie) {
            this.storageTrie = storageTrie;
            setName("_db_set");
            setType(new FunctionType(
                    Arrays.asList(ValueType.I32, ValueType.I32, ValueType.I32, ValueType.I32),
                    Collections.emptyList()
            ));
        }

        @Override
        public long[] execute(long... longs) {
            byte[] key = loadMemory((int)longs[0], (int) longs[1]);
            byte[] val = loadMemory((int)longs[2], (int) longs[3]);
            storageTrie.put(key, val);
            return new long[0];
        }
    }


    public static class DBHas extends HostFunction{
        private final Trie<byte[], byte[]> storageTrie;

        public DBHas(Trie<byte[], byte[]> storageTrie) {
            this.storageTrie = storageTrie;
            setName("_db_has");
            setType(new FunctionType(
                    Arrays.asList(ValueType.I32, ValueType.I32),
                    Collections.singletonList(ValueType.I64)
            ));
        }

        @Override
        public long[] execute(long... longs) {
            byte[] key = loadMemory((int)longs[0], (int) longs[1]);
            return new long[]{
                    storageTrie.containsKey(key) ? 1 : 0
            };
        }
    }
}

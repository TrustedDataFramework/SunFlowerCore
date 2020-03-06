package org.tdf.sunflower.vm.hosts;

import lombok.AllArgsConstructor;
import org.tdf.lotusvm.runtime.HostFunction;
import org.tdf.lotusvm.types.FunctionType;
import org.tdf.lotusvm.types.ValueType;
import org.tdf.rlp.RLPCodec;
import org.tdf.sunflower.vm.abi.Context;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@AllArgsConstructor
public class ContextHelper {
    private Context context;

    public List<HostFunction> getHelpers() {
        return Collections.singletonList(
                new ContextHost(context)
        );
    }

    private static class ContextHost extends HostFunction {
        private byte[] encoded;

        public ContextHost(Context context) {
            setName("_context");
            setType(new FunctionType(Arrays.asList(ValueType.I32, ValueType.I64), Collections.singletonList(ValueType.I64)));
            this.encoded = RLPCodec.encode(context);
        }

        @Override
        public long[] execute(long... parameters) {
            if(parameters[1] != 0){
                putMemory((int) parameters[0], encoded);
            }
            return new long[]{encoded.length};
        }
    }
}

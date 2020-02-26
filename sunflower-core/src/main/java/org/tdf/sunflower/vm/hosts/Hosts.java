package org.tdf.sunflower.vm.hosts;

import lombok.Getter;
import org.tdf.lotusvm.runtime.HostFunction;
import org.tdf.sunflower.vm.abi.Context;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Hosts {
    @Getter
    private final Set<HostFunction> all = new HashSet<>();

    private Result result = new Result();

    public Hosts() {
        init();
    }

    private void init(){
        all.addAll(Arrays.asList(new Abort(), new Keccak256(), new Log(), result));
        all.addAll(new Decimal().getHelpers());
        all.addAll(new JSONHelper().getHelpers());
    }

    public Hosts withParameters(byte[] payload, boolean available){
        all.addAll(
                Arrays.asList(
                        new Parameters(payload),
                        new ParametersLen(payload),
                        new ParametersAvailable(available)
                )
        );
        return this;
    }

    public Hosts withContext(Context context){
        all.addAll(new ContextHelper(context).getHelpers());
        return this;
    }

    public byte[] getResult(){
        return result.getData();
    }
}

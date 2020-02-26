package org.tdf.sunflower.vm.hosts;

import org.tdf.lotusvm.runtime.HostFunction;
import org.tdf.sunflower.vm.abi.Context;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Hosts {
    private Result result = new Result();

    private boolean disableLog;

    private boolean disableEvent;

    private Context context;

    private List<HostFunction> parameters;

    public Hosts disableLog() {
        this.disableLog = true;
        return this;
    }

    public Hosts disableEvent(){
        this.disableEvent = true;
        return this;
    }

    public Hosts() {

    }

    public Set<HostFunction> getAll() {
        Set<HostFunction> all = new HashSet<>();
        all.addAll(Arrays.asList(new Abort(), new Keccak256(), result));
        all.addAll(new Decimal().getHelpers());
        all.addAll(new JSONHelper().getHelpers());
        if (!disableLog) {
            all.add(new Log());
        } else {
            all.add(new PseudoLog());
        }
        if (context != null) {
            all.addAll(new ContextHelper(context).getHelpers());
        }
        if (parameters != null) {
            all.addAll(parameters);
        }
        return all;
    }


    public Hosts withParameters(byte[] payload, boolean available) {
        parameters =
                Arrays.asList(
                        new Parameters(payload),
                        new ParametersLen(payload),
                        new ParametersAvailable(available)
                )
        ;
        return this;
    }

    public Hosts withContext(Context context) {
        this.context = context;
        return this;
    }

    public byte[] getResult() {
        return result.getData();
    }
}

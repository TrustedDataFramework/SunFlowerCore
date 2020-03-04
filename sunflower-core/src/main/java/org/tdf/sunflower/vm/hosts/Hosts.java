package org.tdf.sunflower.vm.hosts;

import org.tdf.lotusvm.runtime.HostFunction;
import org.tdf.sunflower.vm.abi.Context;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Hosts {
    private Result result = new Result();

    private boolean disableEvent;

    private Context context;

    private ContractDB contractDb;

    private List<HostFunction> parameters;



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
        all.add(new Log());

        if (context != null) {
            all.addAll(new ContextHelper(context).getHelpers());
        }
        if (parameters != null) {
            all.addAll(parameters);
        }
        if(contractDb != null){
            all.addAll(contractDb.getHelpers());
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

    public Hosts withDB(ContractDB contractDb){
        this.contractDb = contractDb;
        return this;
    }

    public byte[] getResult() {
        return result.getData();
    }
}

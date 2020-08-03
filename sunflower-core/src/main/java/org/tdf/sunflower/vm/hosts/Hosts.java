package org.tdf.sunflower.vm.hosts;

import org.tdf.common.util.HexBytes;
import org.tdf.lotusvm.runtime.HostFunction;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.types.Transaction;
import org.tdf.sunflower.vm.abi.Context;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Hosts {
    private Result result = new Result();

    private boolean disableEvent;

    private Context context;

    private ContractDB contractDb;

    private Map<HexBytes, Account> states;

    private Transfer transfer;

    public Hosts disableEvent(){
        this.disableEvent = true;
        return this;
    }

    public Hosts() {
    }

    public Hosts withTransfer(Transfer transfer){
        this.transfer = transfer;
        return this;
    }

    public Hosts withTransfer(
            Map<HexBytes, Account> states,
            Transaction tx,
            HexBytes createdBy,
            HexBytes contractAddress
    ){
        this.transfer = new Transfer(tx.getFromAddress(), states, createdBy, contractAddress);
        return this;
    }

    public Set<HostFunction> getAll() {
        Set<HostFunction> all = new HashSet<>();
        all.addAll(Arrays.asList(new Abort(), new HashHost(), result));
        all.addAll(new Decimal().getHelpers());
        all.addAll(new JSONHelper().getHelpers());
        all.add(new Log());

        if (context != null) {
            all.addAll(new ContextHelper(context).getHelpers());
        }

        if(this.transfer != null){
            all.add(this.transfer);
        }

        if(contractDb != null){
            all.addAll(contractDb.getHelpers());
        }
        return all;
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

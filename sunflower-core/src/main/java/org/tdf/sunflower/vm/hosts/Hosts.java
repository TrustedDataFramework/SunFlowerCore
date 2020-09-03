package org.tdf.sunflower.vm.hosts;

import org.tdf.common.util.HexBytes;
import org.tdf.lotusvm.runtime.HostFunction;
import org.tdf.sunflower.facade.Message;
import org.tdf.sunflower.facade.MessageQueue;
import org.tdf.sunflower.state.Account;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Hosts {
    private Result result = new Result();

    private ContextHost contextHost;

    private DBFunctions dbFunctions;

    private Transfer transfer;

    private Event event;

    private Reflect reflect;

    public Hosts() {
    }

    public Hosts withEvent(MessageQueue<String, Message> mq, HexBytes address) {
        this.event = new Event(mq, address);
        return this;
    }

    public Hosts withTransfer(Transfer transfer) {
        this.transfer = transfer;
        return this;
    }

    public Hosts withCall(Reflect reflect) {
        this.reflect = reflect;
        return this;
    }

    public Hosts withTransfer(
            Map<HexBytes, Account> states,
            HexBytes contractAddress
    ) {
        this.transfer = new Transfer(states, contractAddress);
        return this;
    }

    public Set<HostFunction> getAll() {
        Set<HostFunction> all = new HashSet<>(
                Arrays.asList(
                        new Abort(), new HashHost(), result,
                        new Log(), new RLPHost(),
                        new Util()
                )
        );

        if (event != null)
            all.add(this.event);

        if (contextHost != null) {
            all.add(contextHost);
        }

        if (this.transfer != null) {
            all.add(this.transfer);
        }

        if (dbFunctions != null) {
            all.add(dbFunctions);
        }
        return all;
    }

    public Hosts withContext(ContextHost host) {
        this.contextHost = host;
        return this;
    }

    public Hosts withDB(DBFunctions dbFunctions) {
        this.dbFunctions = dbFunctions;
        return this;
    }

    public byte[] getResult() {
        return result.getData();
    }
}

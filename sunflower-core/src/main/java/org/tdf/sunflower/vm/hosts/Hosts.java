package org.tdf.sunflower.vm.hosts;

import lombok.Getter;
import org.tdf.common.util.HexBytes;
import org.tdf.lotusvm.runtime.HostFunction;
import org.tdf.lotusvm.types.FunctionType;
import org.tdf.lotusvm.types.ValueType;
import org.tdf.sunflower.state.Account;

import java.util.*;

public class Hosts {
    public static class Nop extends HostFunction{
        public Nop() {
            setName("_nop");
            setType(
                    new FunctionType(
                            Arrays.asList
                                    (ValueType.I64, ValueType.I64),
                            Collections.emptyList()
                    )
            );
        }

        @Override
        public long[] execute(long... longs) {
            return new long[0];
        }
    }

    private ContextHost contextHost;

    private DBFunctions dbFunctions;

    private Transfer transfer;

    @Getter
    private EventHost eventHost;

    private Reflect reflect;

    public Hosts() {
    }

    public Hosts withEvent(HexBytes address, boolean readonly) {
        this.eventHost = new EventHost(address, readonly);
        return this;
    }

    public Hosts withReflect(Reflect reflect) {
        this.reflect = reflect;
        return this;
    }

    public Hosts withTransfer(
            Map<HexBytes, Account> states,
            HexBytes contractAddress,
            boolean readonly
    ) {
        this.transfer = new Transfer(states, contractAddress, readonly);
        return this;
    }

    public Set<HostFunction> getAll() {
        Set<HostFunction> all = new HashSet<>(
                Arrays.asList(
                        new Log(), new Nop()
                )
        );

        if (reflect != null)
            all.add(this.reflect);

        if (eventHost != null)
            all.add(this.eventHost);

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
}

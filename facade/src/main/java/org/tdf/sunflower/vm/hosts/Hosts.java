package org.tdf.sunflower.vm.hosts;

import lombok.Getter;
import org.tdf.common.util.HexBytes;
import org.tdf.lotusvm.runtime.HostFunction;
import org.tdf.lotusvm.types.FunctionType;
import org.tdf.lotusvm.types.ValueType;
import org.tdf.sunflower.vm.Backend;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Hosts {
    private ContextHost contextHost;
    private DBFunctions dbFunctions;
    private Transfer transfer;
    @Getter
    private EventHost eventHost;
    private Reflect reflect;
    private U256Host u256Host;

    public Hosts() {
        this.u256Host = new U256Host();
    }

    public Hosts withEvent(Backend backend, HexBytes address) {
        this.eventHost = new EventHost(backend, address);
        return this;
    }

    public Hosts withReflect(Reflect reflect) {
        this.reflect = reflect;
        return this;
    }

    public Hosts withTransfer(
        Backend backend,
        HexBytes contractAddress
    ) {
        this.transfer = new Transfer(backend, contractAddress);
        return this;
    }

    public Set<HostFunction> getAll() {
        Set<HostFunction> all = new HashSet<>(
            Arrays.asList(
                new Log(), new Nop()
            )
        );

        if (u256Host != null)
            all.add(this.u256Host);

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

    public static class Nop extends HostFunction {
        static final FunctionType FUNCTION_TYPE = new FunctionType(
            Arrays.asList
                (ValueType.I64, ValueType.I64),
            Collections.emptyList()
        );

        public Nop() {
            super("_nop", FUNCTION_TYPE);
        }

        @Override
        public long execute(long... longs) {
            return 0;
        }
    }
}

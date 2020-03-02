package org.tdf.sunflower.vm.hosts;

import lombok.AllArgsConstructor;
import org.tdf.lotusvm.runtime.HostFunction;
import org.tdf.lotusvm.types.FunctionType;
import org.tdf.lotusvm.types.ValueType;
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
        return Arrays.asList(
                new ContextTransactionHash(context),
                new ContextTransactionHashLen(context),
                new ContextMethod(context),
                new ContextMethodLen(context),
                new ContextSender(context),
                new ContextSenderLen(context),
                new ContextRecipient(context),
                new ContextRecipientLen(context),
                new ContextAmount(context),
                new ContextGasPrice(context),
                new ContextBlockTimestamp(context),
                new ContextTransactionTimestamp(context),
                new ContextBlockHeight(context),
                new ContextParentBlockHash(context),
                new ContextParentBlockHashLen(context),
                new ContextNonce(context),
                new ContextSignature(context),
                new ContextSignatureLen(context),
                new ContextAvailable(context),
                new ContractAddressLen(context),
                new ContractAddress(context),
                new ContractCreatedBy(context),
                new ContractCreatedByLen(context)
        );
    }

    private static class ContextTransactionHash extends HostFunction {
        private Context context;

        public ContextTransactionHash(Context context) {
            setName("_context_transaction_hash");
            setType(new FunctionType(Collections.singletonList(ValueType.I32), new ArrayList<>()));
            this.context = context;
        }

        @Override
        public long[] execute(long... parameters) {
            putMemory((int) parameters[0], context.getTransactionHash().getBytes());
            return new long[0];
        }
    }

    private static class ContextTransactionHashLen extends HostFunction {
        private Context context;

        public ContextTransactionHashLen(Context context) {
            setName("_context_transaction_hash_len");
            setType(new FunctionType(new ArrayList<>(), Collections.singletonList(ValueType.I32)));
            this.context = context;
        }

        @Override
        public long[] execute(long... parameters) {
            return new long[]{context.getTransactionHash().size()};
        }
    }


    private static class ContextMethod extends HostFunction {
        private Context context;

        public ContextMethod(Context context) {
            setName("_context_method");
            setType(new FunctionType(Collections.singletonList(ValueType.I32), new ArrayList<>()));
            this.context = context;
        }

        @Override
        public long[] execute(long... parameters) {
            putStringIntoMemory((int) parameters[0], context.getMethod());
            return new long[0];
        }
    }

    private static class ContextMethodLen extends HostFunction {
        private Context context;

        public ContextMethodLen(Context context) {
            setName("_context_method_len");
            setType(new FunctionType(new ArrayList<>(), Collections.singletonList(ValueType.I32)));
            this.context = context;
        }

        @Override
        public long[] execute(long... parameters) {
            return new long[]{context.getMethod().getBytes(StandardCharsets.UTF_8).length};
        }
    }

    private static class ContextSender extends HostFunction {
        private Context context;

        public ContextSender(Context context) {
            setName("_context_sender");
            setType(new FunctionType(Collections.singletonList(ValueType.I32), new ArrayList<>()));
            this.context = context;
        }

        @Override
        public long[] execute(long... parameters) {
            putMemory((int) parameters[0], context.getSender().getBytes());
            return new long[0];
        }
    }

    private static class ContextSenderLen extends HostFunction {
        private Context context;

        public ContextSenderLen(Context context) {
            setName("_context_sender_len");
            setType(new FunctionType(new ArrayList<>(), Collections.singletonList(ValueType.I32)));
            this.context = context;
        }

        @Override
        public long[] execute(long... parameters) {
            return new long[]{context.getSender().size()};
        }
    }

    private static class ContextRecipient extends HostFunction {
        private Context context;

        public ContextRecipient(Context context) {
            setName("_context_recipient");
            setType(new FunctionType(Collections.singletonList(ValueType.I32), new ArrayList<>()));
            this.context = context;
        }

        @Override
        public long[] execute(long... parameters) {
            putMemory((int) parameters[0], context.getRecipient().getBytes());
            return new long[0];
        }
    }

    private static class ContextRecipientLen extends HostFunction {
        private Context context;

        public ContextRecipientLen(Context context) {
            setName("_context_recipient_len");
            setType(new FunctionType(new ArrayList<>(), Collections.singletonList(ValueType.I32)));
            this.context = context;
        }

        @Override
        public long[] execute(long... parameters) {
            return new long[]{context.getRecipient().size()};
        }
    }

    private static class ContextAmount extends HostFunction {
        private Context context;

        public ContextAmount(Context context) {
            setName("_context_amount");
            setType(new FunctionType(new ArrayList<>(), Collections.singletonList(ValueType.I32)));
            this.context = context;
        }

        @Override
        public long[] execute(long... parameters) {
            return new long[]{context.getAmount()};
        }
    }

    private static class ContextGasPrice extends HostFunction {
        private Context context;

        public ContextGasPrice(Context context) {
            setName("_context_gas_price");
            setType(new FunctionType(new ArrayList<>(), Collections.singletonList(ValueType.I32)));
            this.context = context;
        }

        @Override
        public long[] execute(long... parameters) {
            return new long[]{context.getGasPrice()};
        }
    }

    private static class ContextBlockTimestamp extends HostFunction {
        private Context context;

        public ContextBlockTimestamp(Context context) {
            setName("_context_block_timestamp");
            setType(new FunctionType(new ArrayList<>(), Collections.singletonList(ValueType.I32)));
            this.context = context;
        }

        @Override
        public long[] execute(long... parameters) {
            return new long[]{context.getBlockTimestamp()};
        }
    }

    private static class ContextTransactionTimestamp extends HostFunction {
        private Context context;

        public ContextTransactionTimestamp(Context context) {
            setName("_context_transaction_timestamp");
            setType(new FunctionType(new ArrayList<>(), Collections.singletonList(ValueType.I32)));
            this.context = context;
        }

        @Override
        public long[] execute(long... parameters) {
            return new long[]{context.getTransactionTimestamp()};
        }
    }

    private static class ContextBlockHeight extends HostFunction {
        private Context context;

        public ContextBlockHeight(Context context) {
            setName("_context_block_height");
            setType(new FunctionType(new ArrayList<>(), Collections.singletonList(ValueType.I32)));
            this.context = context;
        }

        @Override
        public long[] execute(long... parameters) {
            return new long[]{context.getBlockHeight()};
        }
    }

    private static class ContextParentBlockHash extends HostFunction {
        private Context context;

        public ContextParentBlockHash(Context context) {
            setName("_context_parent_block_hash");
            setType(new FunctionType(Collections.singletonList(ValueType.I32), new ArrayList<>()));
            this.context = context;
        }

        @Override
        public long[] execute(long... parameters) {
            putMemory((int) parameters[0], context.getParentBlockHash().getBytes());
            return new long[0];
        }
    }

    private static class ContextParentBlockHashLen extends HostFunction {
        private Context context;

        public ContextParentBlockHashLen(Context context) {
            setName("_context_parent_block_hash_len");
            setType(new FunctionType(new ArrayList<>(), Collections.singletonList(ValueType.I32)));
            this.context = context;
        }

        @Override
        public long[] execute(long... parameters) {
            return new long[]{context.getParentBlockHash().size()};
        }
    }

    private static class ContextNonce extends HostFunction {
        private Context context;

        public ContextNonce(Context context) {
            setName("_context_nonce");
            setType(new FunctionType(Collections.emptyList(), Collections.singletonList(ValueType.I64)));
            this.context = context;
        }

        @Override
        public long[] execute(long... parameters) {
            return new long[]{context.getNonce()};
        }
    }

    private static class ContextSignatureLen extends HostFunction {
        private Context context;

        public ContextSignatureLen(Context context) {
            setName("_context_signature_len");
            setType(new FunctionType(Collections.emptyList(), Collections.singletonList(ValueType.I32)));
            this.context = context;
        }

        @Override
        public long[] execute(long... parameters) {
            return new long[]{context.getSignature().size()};
        }
    }

    private static class ContextSignature extends HostFunction {
        private Context context;

        public ContextSignature(Context context) {
            setName("_context_signature");
            setType(new FunctionType(Collections.singletonList(ValueType.I32), Collections.emptyList()));
            this.context = context;
        }

        @Override
        public long[] execute(long... parameters) {
            putMemory((int) parameters[0], context.getSignature().getBytes());
            return new long[0];
        }
    }

    private static class ContextAvailable extends HostFunction {
        private Context context;

        public ContextAvailable(Context context) {
            setName("_context_available");
            setType(new FunctionType(Collections.emptyList(), Collections.singletonList(ValueType.I64)));
            this.context = context;
        }

        @Override
        public long[] execute(long... parameters) {
            return new long[]{context.isAvailable() ? 1 : 0};
        }
    }


    private static class ContractAddressLen extends HostFunction {
        private Context context;

        public ContractAddressLen(Context context) {
            setName("_contract_address_len");
            setType(new FunctionType(Collections.emptyList(), Collections.singletonList(ValueType.I32)));
            this.context = context;
        }

        @Override
        public long[] execute(long... parameters) {
            return new long[]{context.getContractAddress().size()};
        }
    }

    private static class ContractAddress extends HostFunction {
        private Context context;

        public ContractAddress(Context context) {
            setName("_contract_address");
            setType(new FunctionType(Collections.singletonList(ValueType.I32), Collections.emptyList()));
            this.context = context;
        }

        @Override
        public long[] execute(long... parameters) {
            putMemory((int) parameters[0], context.getContractAddress().getBytes());
            return new long[0];
        }
    }

    private static class ContractCreatedByLen extends HostFunction {
        private Context context;

        public ContractCreatedByLen(Context context) {
            setName("_contract_created_by_len");
            setType(new FunctionType(Collections.emptyList(), Collections.singletonList(ValueType.I32)));
            this.context = context;
        }

        @Override
        public long[] execute(long... parameters) {
            return new long[]{context.getCreatedBy().size()};
        }
    }

    private static class ContractCreatedBy extends HostFunction {
        private Context context;

        public ContractCreatedBy(Context context) {
            setName("_contract_created_by");
            setType(new FunctionType(Collections.singletonList(ValueType.I32), Collections.emptyList()));
            this.context = context;
        }

        @Override
        public long[] execute(long... parameters) {
            putMemory((int) parameters[0], context.getCreatedBy().getBytes());
            return new long[0];
        }
    }
}

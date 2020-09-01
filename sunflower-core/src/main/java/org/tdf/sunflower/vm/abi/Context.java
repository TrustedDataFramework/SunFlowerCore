package org.tdf.sunflower.vm.abi;

import com.google.common.primitives.Bytes;
import lombok.*;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.SafeMath;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.types.Transaction;

import java.nio.charset.StandardCharsets;


@Getter
public class Context {
    private ContextHeader header;
    private ContextTransaction transaction;
    private ContextContract contextContract;
    private HexBytes arguments;

    public boolean containsTransaction(){
        return transaction != null;
    }

    public Context(
            ContextHeader header,
            ContextTransaction transaction,
            ContextContract contextContract,
            HexBytes arguments
    ) {
        this.header = header;
        this.transaction = transaction;
        this.contextContract = contextContract;
        this.arguments = arguments;
    }

    public Context(Header header, Transaction transaction, Account contractAccount){
        this(
                header == null ? null : new ContextHeader(header),
                transaction == null ? null : new ContextTransaction(transaction),
                contractAccount == null ? null : new ContextContract(contractAccount),
                transaction.getPayload()
        );
    }

    public Context(Header header, Transaction transaction, Account contractAccount, HexBytes bytes){
        this(
                header == null ? null : new ContextHeader(header),
                transaction == null ? null : new ContextTransaction(transaction),
                contractAccount == null ? null : new ContextContract(contractAccount),
                bytes
        );
    }

    public String getMethod(){
        if(transaction != null && transaction.getType() == Transaction.Type.CONTRACT_DEPLOY.code)
            return "init";
        return readMethod(arguments);
    }

    public static String readMethod(HexBytes bytes){
        int len = bytes.getBytes()[0];
        byte[] arr = new byte[len];
        System.arraycopy(bytes.getBytes(), 1, arr, 0, len);
        return new String(arr, StandardCharsets.US_ASCII);
    }

    public static byte[] readParameters(HexBytes bytes){
        int len = bytes.getBytes()[0];
        byte[] arr = new byte[bytes.size() - 1 - len];
        System.arraycopy(bytes.getBytes(), 1 + len, arr, 0, arr.length);
        return arr;
    }

    public static HexBytes buildArguments(String method, byte[] parameters){
        byte[] m = method.getBytes(StandardCharsets.US_ASCII);
        int len = 1 + m.length + parameters.length;
        byte[] ret = new byte[len];
        ret[0] = (byte) m.length;
        System.arraycopy(m, 0, ret, 1, m.length);
        System.arraycopy(parameters, 0, ret, 1 + m.length, parameters.length);
        return HexBytes.fromBytes(ret);
    }
}

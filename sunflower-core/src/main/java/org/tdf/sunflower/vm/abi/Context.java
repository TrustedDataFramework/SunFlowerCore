package org.tdf.sunflower.vm.abi;

import lombok.*;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.types.Transaction;

import java.nio.charset.StandardCharsets;


@Getter
public class Context {
    private ContextHeader header;
    private ContextTransaction transaction;
    private ContextContract contextContract;
    private HexBytes viewParameters;

    public boolean containsTransaction(){
        return transaction != null;
    }

    public Context(ContextHeader header, ContextTransaction transaction, ContextContract contextContract, HexBytes viewParameters) {
        this.header = header;
        this.transaction = transaction;
        this.contextContract = contextContract;
        this.viewParameters = viewParameters;
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
        if(transaction != null){
            return readMethod(transaction.getPayload());
        }
        return readMethod(viewParameters);
    }

    public static String readMethod(HexBytes bytes){
        int len = bytes.getBytes()[0];
        byte[] arr = new byte[len];
        System.arraycopy(bytes.getBytes(), 1, arr, 0, len);
        return new String(arr, StandardCharsets.US_ASCII);
    }
}

package org.tdf.sunflower.vm.abi;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.types.Transaction;

import java.nio.charset.StandardCharsets;


@Getter
@AllArgsConstructor
public class Context {
    private Header header;
    private Transaction transaction;
    private Account contractAccount;
    private HexBytes arguments;
    private HexBytes msgSender;
    private long amount;

    public boolean containsTransaction() {
        return transaction != null;
    }


    public String getMethod() {
        if (transaction != null && transaction.getType() == Transaction.Type.CONTRACT_DEPLOY.code)
            return "init";
        return readMethod(arguments);
    }

    public byte[] getParameters(){
        return readParameters(this.arguments);
    }

    public static String readMethod(HexBytes arguments) {
        int len = arguments.getBytes()[0];
        byte[] arr = new byte[len];
        System.arraycopy(arguments.getBytes(), 1, arr, 0, len);
        return new String(arr, StandardCharsets.US_ASCII);
    }

    public static byte[] readParameters(HexBytes arguments) {
        int len = arguments.getBytes()[0];
        byte[] arr = new byte[arguments.size() - 1 - len];
        System.arraycopy(arguments.getBytes(), 1 + len, arr, 0, arr.length);
        return arr;
    }

    public static HexBytes buildArguments(String method, byte[] parameters) {
        byte[] m = method.getBytes(StandardCharsets.US_ASCII);
        int len = 1 + m.length + parameters.length;
        byte[] ret = new byte[len];
        ret[0] = (byte) m.length;
        System.arraycopy(m, 0, ret, 1, m.length);
        System.arraycopy(parameters, 0, ret, 1 + m.length, parameters.length);
        return HexBytes.fromBytes(ret);
    }
}

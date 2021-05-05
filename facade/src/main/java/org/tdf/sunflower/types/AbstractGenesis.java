package org.tdf.sunflower.types;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import org.tdf.common.types.Uint256;
import org.tdf.common.util.BigIntegers;
import org.tdf.common.util.ByteUtil;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.state.Account;

import java.math.BigInteger;
import java.util.*;

@AllArgsConstructor
public abstract class AbstractGenesis {
    protected JsonNode parsed;

    public abstract Block getBlock();

    public long getTimestamp() {
        JsonNode n = parsed.get("timestamp");
        return n == null ? 0: n.asLong();
    }

    public HexBytes getParentHash() {
        JsonNode n = parsed.get("gasLimit");
        return n == null ? ByteUtil.ZEROS_32 : HexBytes.fromHex(n.asText());
    }

    public long getGasLimit() {
        JsonNode n = parsed.get("gasLimit");
        return n == null ? 0 : n.asLong();
    }

    public HexBytes getGasLimitHex() {
        return HexBytes.fromBytes(ByteUtil.longToBytesNoLeadZeroes(getGasLimit()));
    }

    public List<Account> getAlloc() {
        JsonNode alloc = parsed.get("alloc");

        if(alloc == null)
            return Collections.emptyList();

        List<Account> r = new ArrayList<>();
        for (Iterator<String> it = alloc.fieldNames(); it.hasNext(); ) {
            String k = it.next();
            String v = alloc.get(k).asText();
            BigInteger b = v.startsWith("0x") ? new BigInteger(v.substring(2), 16) : new BigInteger(v);
            Uint256 balance = Uint256.of(BigIntegers.asUnsignedByteArray(b));
            r.add(Account.emptyAccount(HexBytes.fromHex(k), balance));
        }
        return r;
    }

    protected List<JsonNode> getArray(String field) {
        JsonNode n = parsed.get(field);
        if(n == null || n.isNull())
            return Collections.emptyList();

        List<JsonNode> li = new ArrayList<>();

        for(int i = 0; i < n.size(); i++) {
            li.add(n.get(i));
        }

        return li;
    }
}

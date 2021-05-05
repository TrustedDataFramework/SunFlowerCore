package org.tdf.sunflower.types;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.tdf.common.util.HexBytes;

import java.util.Properties;

@Value
public class PropertyReader {
    Properties parsed;

    public String getAsLowerCased(String property) {
        String s = parsed.getProperty(property);
        return (s == null ? "" : s).trim().toLowerCase();
    }

    public String getAsNonNull(String property) {
        String s = parsed.getProperty(property);
        return (s == null ? "" : s).trim();
    }

    public boolean getAsBool(String property) {
        return "true".equals(getAsLowerCased(property));
    }

    public int getAsInt(String property) {
        String s = parsed.getProperty(property);
        if(s == null)
            throw new RuntimeException("read property " + property + " failed, property not found");
        return Integer.parseInt(s.trim());
    }

    public int getAsInt(String property, int defaultValue) {
        String s = parsed.getProperty(property);
        if(s == null || s.trim().isEmpty())
            return defaultValue;
        return Integer.parseInt(s.trim());
    }

    public HexBytes getAsPrivate(String property) {
        String s = parsed.getProperty(property);
        if(s == null)
            throw new RuntimeException("property " + property + " not found");
        HexBytes k = HexBytes.fromHex(s.trim());
        if(k.size() != 32)
            throw new RuntimeException("invalid private key size: " + k);
        return k;
    }


    public HexBytes getAsAddress(String property) {
        String s = parsed.getProperty(property);
        HexBytes minerCoinBase = HexBytes.fromHex(s);
        if(minerCoinBase.size() != Transaction.ADDRESS_LENGTH)
            throw new RuntimeException("invalid coinbase address " + minerCoinBase);
        return minerCoinBase;
    }
}

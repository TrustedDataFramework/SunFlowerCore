package org.tdf.sunflower.types;

import lombok.Value;
import org.tdf.common.types.Uint256;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.facade.PropertyLike;

import java.math.BigInteger;


@Value
public class PropertyReader {
    PropertyLike properties;

    public String getAsLowerCased(String property) {
        String s = properties.getProperty(property);
        return (s == null ? "" : s).trim().toLowerCase();
    }

    public String getAsNonNull(String property) {
        String s = properties.getProperty(property);
        return (s == null ? "" : s).trim();
    }

    public boolean getAsBool(String property) {
        return "true".equals(getAsLowerCased(property));
    }

    public int getAsInt(String property) {
        String s = properties.getProperty(property);
        if (s == null)
            throw new RuntimeException("read property " + property + " failed, property not found");
        return Integer.parseInt(s.trim());
    }

    public Uint256 getAsU256(String property, Uint256 defaultValue) {
        String s = properties.getProperty(property);
        if (s == null || s.trim().isEmpty()) {
            return Uint256.ZERO;
        }
        s = s.trim().toLowerCase();
        BigInteger b = s.startsWith("0x") ? new BigInteger(s.substring(2), 16) : new BigInteger(s);
        return Uint256.of(b);
    }

    public int getAsInt(String property, int defaultValue) {
        String s = properties.getProperty(property);
        if (s == null || s.trim().isEmpty())
            return defaultValue;
        return Integer.parseInt(s.trim());
    }

    public HexBytes getAsPrivate(String property) {
        String s = properties.getProperty(property);
        if (s == null)
            throw new RuntimeException("property " + property + " not found");
        HexBytes k = HexBytes.fromHex(s.trim());
        if (k.size() != 32)
            throw new RuntimeException("invalid private key size: " + k);
        return k;
    }

    public long getAsLong(String property) {
        String s = properties.getProperty(property);
        if (s == null || s.trim().isEmpty())
            throw new RuntimeException("read property " + property + " failed, property not found");
        return Long.parseLong(s.trim());
    }

    public HexBytes getAsAddress(String property) {
        String s = properties.getProperty(property);
        HexBytes minerCoinBase = HexBytes.fromHex(s);
        if (minerCoinBase.size() != Transaction.ADDRESS_LENGTH)
            throw new RuntimeException("invalid coinbase address " + minerCoinBase);
        return minerCoinBase;
    }
}

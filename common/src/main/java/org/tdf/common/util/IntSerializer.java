package org.tdf.common.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.tdf.common.types.Uint256;

import java.io.IOException;
import java.math.BigInteger;

public class IntSerializer extends StdSerializer<Number> {
    private static final BigInteger MAX_SAFE_INTEGER = BigInteger.valueOf(9007199254740991L);
    private static final BigInteger MIN_SAFE_INTEGER = BigInteger.valueOf(-9007199254740991L);

    public IntSerializer() {
        super(Number.class);
    }

    @Override
    public void serialize(Number value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (value instanceof Uint256)
            value = ((Uint256) value).value();
        if (value instanceof Long || value instanceof Integer || value instanceof Byte || value instanceof Short)
            value = BigInteger.valueOf((long) value);

        BigInteger val = (BigInteger) value;
        if (val.compareTo(MAX_SAFE_INTEGER) > 0 || val.compareTo(MIN_SAFE_INTEGER) < 0) {
            gen.writeString(val.toString(10));
        } else {
            gen.writeNumber(val.longValueExact());
        }
    }
}

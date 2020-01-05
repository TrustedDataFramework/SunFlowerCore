package org.tdf.common.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.apache.commons.codec.binary.Hex;

import java.io.IOException;

class HexBytesUtils {
    static class HexBytesSerializer extends StdSerializer<HexBytes> {
        public HexBytesSerializer() {
            super(HexBytes.class);
        }

        @Override
        public void serialize(HexBytes value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            jgen.writeString(Hex.encodeHexString(value.getBytes()));
        }
    }

    static class HexBytesDeserializer extends StdDeserializer<HexBytes> {
        public HexBytesDeserializer() {
            super(HexBytes.class);
        }

        @Override
        public HexBytes deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.getCodec().readTree(p);
            if (node.isNull()) return HexBytes.empty();
            String encoded = node.asText();
            if (encoded == null || encoded.equals("")) {
                return HexBytes.empty();
            }
            if (encoded.startsWith("0x")) {
                encoded = encoded.substring(2);
            }
            return HexBytes.fromHex(encoded);
        }

        private static class HexBytesDeserializeException extends JsonProcessingException {
            protected HexBytesDeserializeException(String msg) {
                super(msg);
            }
        }
    }
}

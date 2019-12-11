package org.tdf.sunflower.consensus.vrf;


import org.tdf.rlp.RLPDeserializer;
import org.tdf.rlp.RLPElement;
import org.tdf.rlp.RLPItem;
import org.tdf.rlp.RLPList;
import org.tdf.sunflower.consensus.vrf.struct.VrfBlockFields;

import java.lang.reflect.ParameterizedType;

public class Example {
    static byte[] buildMessage(int Object, int code) {
        RLPList list = RLPElement.encode(Object).getAsList();
        list.add(0, RLPItem.fromInt(code));
        return list.getEncoded();
    }

    static Object parseFrom(byte[] msg) {
        RLPList list = RLPElement.fromEncoded(msg).getAsList();
        int code = list.get(0).getAsItem().getInt();
        list = list.subList(1, list.size());
        switch (code) {
            case 0:
                return RLPDeserializer.deserialize(list, VrfBlockFields.class);
        }
        throw new RuntimeException("not implemented");
    }
}

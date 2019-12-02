package org.wisdom.consortium.vm.wasm.types;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.wisdom.consortium.vm.wasm.BytesReader;

/**
 * Global types are encoded by their value type and a flag for their mutability.
 * globaltype ::= 𝑡:valtype 𝑚:mut ⇒ 𝑚 𝑡
 * mut ::= 0x00 ⇒ const
 * | 0x01 ⇒ var
 */
@AllArgsConstructor
@Builder
@Getter
public class GlobalType {
    private ValueType valueType;
    private boolean mutable; // true var ,false const

    public static GlobalType readFrom(BytesReader reader){
        ValueType valueType = ValueType.readFrom(reader);
        boolean mutable = reader.read() != 0;
        return GlobalType.builder().valueType(valueType).mutable(mutable).build();
    }

}

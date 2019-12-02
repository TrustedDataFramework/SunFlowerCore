package org.wisdom.consortium.vm.wasm.section;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.wisdom.consortium.vm.wasm.BytesReader;

/**
 * Each section consists of
 * • a one-byte section id,
 * • the u32 size of the contents, in bytes,
 * • the actual contents, whose structure is depended on the section id.
 *
 *
 */
@AllArgsConstructor
public abstract class Section {
   @Getter
   private SectionID id;
   @Getter
   private long size; // unsigned integer
   @Getter(AccessLevel.PROTECTED)
   private BytesReader payload;


   abstract void readPayload();

   // clean payload after read
   public void clearPayload(){
       payload = null;
   }
}

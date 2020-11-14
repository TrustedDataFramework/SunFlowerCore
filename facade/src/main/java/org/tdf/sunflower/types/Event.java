package org.tdf.sunflower.types;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.tdf.rlp.RLPList;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Event {
    private String name;
    private RLPList fields;
}

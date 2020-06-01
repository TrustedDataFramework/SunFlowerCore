package org.tdf.sunflower.consensus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.tdf.common.util.HexBytes;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Proposer {

    private HexBytes address;

    private long startTimeStamp;

    private long endTimeStamp;
}

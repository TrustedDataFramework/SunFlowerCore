package org.tdf.sunflower.consensus.vrf.struct;

import lombok.*;
import org.tdf.rlp.RLP;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VrfBlockFields {

    @RLP(0)
    private byte[] seed;

    @RLP(1)
    private byte[] priority;

    @RLP(2)
    private byte[] miner;

    @RLP(3)
    private byte[] proposalProof;

    @RLP(4)
    // RLP encoding strings, comma separated.
    private String parentReductionCommitProofs;

    @RLP(5)
    // RLP encoding strings, comma separated.
    private String parentFinalCommitProofs;

}

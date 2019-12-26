package org.tdf.sunflower.consensus.vrf.struct;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.tdf.rlp.RLP;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VrfBlockFields {

    @RLP(0)
    private byte[] nonce;

    @RLP(1)
    private byte[] priority;

    @RLP(2)
    private byte[] miner;

    @RLP(3)
    private byte[] proposalProof;

}

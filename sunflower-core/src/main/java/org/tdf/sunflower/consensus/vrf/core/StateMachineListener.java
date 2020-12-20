package org.tdf.sunflower.consensus.vrf.core;

import org.apache.commons.codec.DecoderException;

import java.io.IOException;

/**
 * @author James Hu
 * @since 2019/6/22
 */
public interface StateMachineListener {

    // This is a callback for VrfStateMachine to notify its changed state.
    void stateChanged(int from, int to) throws DecoderException, IOException;

    // This is a callback for VrfStateMachine to finalize that if blockchain system
    // can accept it as final block.
    // TODO: DO NOT implement it as a blocking logic, that will impact
    // VrfStateMachine performance.
    boolean finalizeNewBlock(VrfRound vrfRound, VrfBlockWrapper finalBlock);
}
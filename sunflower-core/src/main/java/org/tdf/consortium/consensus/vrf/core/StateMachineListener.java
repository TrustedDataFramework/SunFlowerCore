package org.tdf.consortium.consensus.vrf.core;

import org.apache.commons.codec.DecoderException;

/**
 * @author James Hu
 * @since 2019/6/22
 */
public interface StateMachineListener {

	// This is a callback for VrfStateMachine to notify its changed state.
	public void stateChanged(int from, int to) throws DecoderException;

	// This is a callback for VrfStateMachine to finalize that if blockchain system can accept it as final block.
	// TODO: DO NOT implement it as a blocking logic, that will impact VrfStateMachine performance.
	public boolean finalizeNewBlock(VrfRound vrfRound, VrfBlockWrapper finalBlock);
}
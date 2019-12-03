package org.tdf.sunflower.consensus.vrf.core;

import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tdf.sunflower.consensus.vrf.VrfStateMachine;

/**
 * @author James Hu
 * @since 2019/6/20
 */
public class StateTransitTimerTask extends TimerTask {

    private static final Logger logger = LoggerFactory.getLogger("StateTransitTimerTask");

	private final int from;
	private final VrfStateMachine stateMachine;
	private final java.util.Timer timer;

	public StateTransitTimerTask(int from, VrfStateMachine stateMachine, java.util.Timer timer) {
		this.from = from;
		this.stateMachine = stateMachine;
		this.timer = timer;
	}

	@Override
	public void run() {
		// Force to do state transit
		int newState = stateMachine.transit(from, timer);
		//logger.info("TimerTask is done for transit: {} -> {}",
		//		VrfStateMachine.getStateName(from), VrfStateMachine.getStateName(newState));
	}
}

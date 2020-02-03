package org.tdf.sunflower.events;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tdf.common.event.EventBus;
import org.tdf.sunflower.facade.MinerListener;
import org.tdf.sunflower.types.Block;

/**
 * Bridge between miner and event bus
 */
@Component
@Slf4j
public class Bridge implements MinerListener {
    private EventBus eventBus;

    public Bridge(EventBus eventBus){
        this.eventBus = eventBus;
    }

    @Override
    public void onBlockMined(Block block) {
        eventBus.publish(new NewBlockMined(block));
    }

    @Override
    public void onMiningFailed(Block block) {
        eventBus.publish(new MiningFailed(block));
    }
}

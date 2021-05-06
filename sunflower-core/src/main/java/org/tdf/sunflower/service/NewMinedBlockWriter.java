package org.tdf.sunflower.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tdf.common.event.EventBus;
import org.tdf.sunflower.events.NewBlockMined;
import org.tdf.sunflower.facade.SunflowerRepository;
import org.tdf.sunflower.types.Block;

@Component
@Slf4j
public class NewMinedBlockWriter {
    public NewMinedBlockWriter(
        SunflowerRepository repository,
        EventBus eventBus
    ) {
        eventBus.subscribe(NewBlockMined.class, (e) -> {
            Block block = e.getBlock();
            if (block == null)
                return;
            repository.writeBlock(block, e.getInfos());
        });
    }
}

package org.tdf.sunflower.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tdf.common.event.EventBus;
import org.tdf.sunflower.events.NewBlockMined;
import org.tdf.sunflower.facade.IRepositoryService;
import org.tdf.sunflower.facade.RepositoryWriter;
import org.tdf.sunflower.types.Block;

@Component
@Slf4j
public class NewMinedBlockWriter {
    public NewMinedBlockWriter(
        IRepositoryService repository,
        EventBus eventBus
    ) {
        eventBus.subscribe(NewBlockMined.class, (e) -> {
            Block block = e.getBlock();
            if (block == null)
                return;
            try (RepositoryWriter writer = repository.getWriter()){
                writer.writeBlock(block, e.getInfos());
            }
        });
    }
}

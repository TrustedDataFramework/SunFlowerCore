package org.tdf.sunflower.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tdf.common.event.EventBus;
import org.tdf.sunflower.events.NewBlockMined;
import org.tdf.sunflower.facade.ConsensusEngineFacade;
import org.tdf.sunflower.facade.SunflowerRepository;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.ValidateResult;

import java.util.Optional;

@Component
@Slf4j
public class NewMinedBlockWriter {
    public NewMinedBlockWriter(
            SunflowerRepository repository,
            EventBus eventBus,
            ConsensusEngineFacade engine
    ) {
        eventBus.subscribe(NewBlockMined.class, (e) -> {
            Block block = e.getBlock();
            Optional<Block> o = repository.getBlock(block.getHashPrev().getBytes());
            if (!o.isPresent()) return;
            ValidateResult result =
                    engine.getValidator()
                    .validate(block, o.get());
            if (result.isSuccess()) {
                repository.writeBlock(block);
                return;
            }
            log.error(result.getReason());
        });
    }
}

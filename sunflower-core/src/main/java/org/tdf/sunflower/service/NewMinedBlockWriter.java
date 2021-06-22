package org.tdf.sunflower.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tdf.common.event.EventBus;
import org.tdf.common.util.FixedDelayScheduler;
import org.tdf.sunflower.events.NewBlockMined;
import org.tdf.sunflower.facade.RepositoryService;
import org.tdf.sunflower.facade.RepositoryWriter;
import org.tdf.sunflower.types.Block;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Component
@Slf4j
public class NewMinedBlockWriter {
    public NewMinedBlockWriter(
        RepositoryService repo,
        EventBus eventBus
    ) {

    }
}

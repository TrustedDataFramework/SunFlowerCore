package org.tdf.sunflower.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tdf.common.event.EventBus;
import org.tdf.sunflower.facade.RepositoryService;

@Component
@Slf4j
public class NewMinedBlockWriter {
    public NewMinedBlockWriter(
        RepositoryService repo,
        EventBus eventBus
    ) {

    }
}

package org.freakz.services.counts;

import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.json.engine.EngineRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CountService {


    @Async
    public void handleCounts(EngineRequest request) {
        log.debug("counts!");
    }


}

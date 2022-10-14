package org.freakz.springboot.ui.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.freakz.springboot.ui.backend.config.TheBotConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TheBotConfigService implements CommandLineRunner {

    private final TheBotConfig config;

    @Autowired
    public TheBotConfigService(TheBotConfig config) {
        this.config = config;
    }


    @Override
    public void run(String... args) throws Exception {
        log.debug("the.bot.runtimeDir: {}", this.config.getRuntimeDir());
        int foo = 0;
    }
}

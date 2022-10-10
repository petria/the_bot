package org.freakz.springboot.ui.backend.service;

import org.freakz.springboot.ui.backend.config.TheBotConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

@Service
public class TheBotConfigService implements CommandLineRunner {

    private final TheBotConfig config;

    @Autowired
    public TheBotConfigService(TheBotConfig config) {
        this.config = config;
    }


    @Override
    public void run(String... args) throws Exception {
        int foo = 0;
    }
}

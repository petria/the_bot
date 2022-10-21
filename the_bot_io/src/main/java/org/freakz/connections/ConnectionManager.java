package org.freakz.connections;


import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.json.IrcServerConfig;
import org.freakz.common.model.json.TheBotConfig;
import org.freakz.config.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;

@Service
@Slf4j
public class ConnectionManager {


    @Autowired
    private ConfigService configService;

    @Autowired
    private EventPublisher eventPublisher;

    @PostConstruct
    public void init() throws IOException {

        TheBotConfig theBotConfig = configService.readBotConfig();
        for (IrcServerConfig config : theBotConfig.getIrcServerConfigs()) {
            log.debug("init IrcServerConfig: {}", config);

            IrcServerConnection isc = new IrcServerConnection(this.eventPublisher);
            isc.init(theBotConfig.getBotConfig().getBotName(), config);

        }
//        log.debug(">> Start IrcServerConnections");
        log.debug("<< done!");
    }


}

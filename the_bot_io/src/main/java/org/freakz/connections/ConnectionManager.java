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
        log.debug(">> Connecting IRC");
        for (IrcServerConfig config : theBotConfig.getIrcServerConfigs()) {
            log.debug("init IrcServerConfig: {}", config);

            IrcServerConnection isc = new IrcServerConnection(this.eventPublisher);
            isc.init(theBotConfig.getBotConfig().getBotName(), config);

        }
//        log.debug(">> Start IrcServerConnections");
        log.debug("<< done!");

        log.debug(">> Connecting DISCORD");
        DiscordServerConnection dsc = new DiscordServerConnection(this.eventPublisher);
        dsc.init(theBotConfig.getBotConfig().getBotName(), theBotConfig.getDiscordConfig());
        log.debug(">> done!");

    }
//https://discord.com/oauth2/authorize?client_id=288964147721404416&scope=bot%20applications.commands&permissions=0&prompt=consent

}

package org.freakz.common.model.json;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
/*
 Structure of master config file
 */
public class TheBotConfig {

    private BotConfig botConfig;
    private IrcServerConfig ircServerConfig;

}

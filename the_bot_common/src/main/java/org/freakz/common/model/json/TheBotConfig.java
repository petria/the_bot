package org.freakz.common.model.json;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Builder
@Data
@NoArgsConstructor
/*
 Structure of master config file
 */
public class TheBotConfig {

    private BotConfig botConfig;
    private IrcServerConfig ircServerConfig;

}

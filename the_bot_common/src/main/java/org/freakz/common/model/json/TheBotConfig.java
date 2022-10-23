package org.freakz.common.model.json;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@Builder
@Data
@NoArgsConstructor
/*
 Structure of master config file
 */
public class TheBotConfig {

    private BotConfig botConfig;
    private DiscordConfig discordConfig;
    private List<IrcServerConfig> ircServerConfigs;

}

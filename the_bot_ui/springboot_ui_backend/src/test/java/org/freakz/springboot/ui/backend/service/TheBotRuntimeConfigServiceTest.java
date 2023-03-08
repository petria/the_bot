package org.freakz.springboot.ui.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.freakz.common.model.json.BotConfig;
import org.freakz.common.model.json.DiscordConfig;
import org.freakz.common.model.json.IrcChannel;
import org.freakz.common.model.json.IrcNetwork;
import org.freakz.common.model.json.IrcServer;
import org.freakz.common.model.json.IrcServerConfig;
import org.freakz.common.model.json.TheBotConfig;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class TheBotRuntimeConfigServiceTest {


    @Test
    public void integration_test_create_config_json() throws JsonProcessingException {
        IrcServerConfig ircServerConfig
                = IrcServerConfig.builder()
                .name("TestConfig")
                .ircNetwork(createIrcNetWork())
                .channelList(createChannelList())
                .build();

        List<IrcServerConfig> configs = new ArrayList<>();
        configs.add(ircServerConfig);

        TheBotConfig botConfig
                = TheBotConfig.builder()
                .botConfig(createBotConfig())
                .discordConfig(createDiscordConfig())
                .ircServerConfigs(configs)
                .build();

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(botConfig);

        System.out.printf("JSON: %s\n", json);
    }

    private DiscordConfig createDiscordConfig() {
        return DiscordConfig.builder()
                .token("1245")
                .build();
    }

    private BotConfig createBotConfig() {
        BotConfig botConfig
                = BotConfig.builder()
                .botName("HokanTheBot")
                .build();
        return botConfig;
    }

    private IrcNetwork createIrcNetWork() {

        IrcServer ircServer = IrcServer
                .builder()
                .host("localhost")
                .port(6667)
                .build();

        IrcNetwork network
                = IrcNetwork.builder()
                .name("TestNetwork")
                .ircServer(ircServer)
                .build();
        return network;
    }

    private List<IrcChannel> createChannelList() {
        List<IrcChannel> list = new ArrayList<>();

        IrcChannel channel
                = IrcChannel.builder()
                .name("#HokanDEV")
                .build();
        list.add(channel);

        IrcChannel channel2
                = IrcChannel.builder()
                .name("#HokanDEV2")
                .build();
        list.add(channel2);

        return list;
    }


}

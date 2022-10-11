package org.freakz.springboot.ui.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.freakz.springboot.ui.backend.models.json.BotConfig;
import org.freakz.springboot.ui.backend.models.json.IrcChannel;
import org.freakz.springboot.ui.backend.models.json.IrcNetwork;
import org.freakz.springboot.ui.backend.models.json.IrcServer;
import org.freakz.springboot.ui.backend.models.json.IrcServerConfig;
import org.freakz.springboot.ui.backend.models.json.TheBotConfig;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class TheBotConfigServiceTest {



    @Test
    public void integration_test_create_config_json() throws JsonProcessingException {
        IrcServerConfig ircServerConfig
                = IrcServerConfig.builder()
                .name("TestConfig")
                .ircNetwork(createIrcNetWork())
                .channelList(createChannelList())
                .build();

        TheBotConfig botConfig
                = TheBotConfig.builder()
                .botConfig(createBotConfig())
                .ircServerConfig(ircServerConfig)
                .build();

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(botConfig);

        System.out.printf("JSON: %s\n", json);
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

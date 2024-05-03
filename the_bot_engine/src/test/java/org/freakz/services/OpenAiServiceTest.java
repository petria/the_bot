package org.freakz.services;

import org.freakz.common.model.botconfig.BotConfig;
import org.freakz.common.model.botconfig.TheBotConfig;
import org.freakz.engine.config.ConfigService;
import org.freakz.engine.services.ai.OpenAiService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.when;


public class OpenAiServiceTest {


    @Test
    public void testOpenAi() {

        ConfigService configService = Mockito.mock(ConfigService.class);
        TheBotConfig theBotConfig = Mockito.mock(TheBotConfig.class);
        BotConfig botConfig = Mockito.mock(BotConfig.class);

        when(configService.readBotConfig()).thenReturn(theBotConfig);
        when(theBotConfig.getBotConfig()).thenReturn(botConfig);

        OpenAiService service = new OpenAiService(configService);
        String s = service.queryAi("Pitäskö kaivaa kaljat kaapista?");
        int foo = 0;

    }

    @Test
    public void testFirstWordReplace() {
        String foo = "Pitääskö sitä käydä kattomas joku leffa";
        String s = foo.replaceFirst("^\\S+", "Tsfdfd");
        if (!s.endsWith("?")) {
            s = s + "?";
        }
        int bar = 0;

    }

}

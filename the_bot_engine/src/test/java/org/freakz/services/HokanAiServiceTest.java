package org.freakz.services;

import static org.mockito.Mockito.when;

import org.freakz.common.model.botconfig.BotConfig;
import org.freakz.common.model.botconfig.TheBotConfig;
import org.freakz.engine.config.ConfigService;
import org.freakz.engine.functions.HokanAiService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class HokanAiServiceTest {

  //    @Test
  public void testOpenAi() {

    ConfigService configService = Mockito.mock(ConfigService.class);
    TheBotConfig theBotConfig = Mockito.mock(TheBotConfig.class);
    BotConfig botConfig = Mockito.mock(BotConfig.class);

    when(configService.readBotConfig()).thenReturn(theBotConfig);
    when(theBotConfig.getBotConfig()).thenReturn(botConfig);
    // tsek
    HokanAiService service = new HokanAiService(null, null, null);
    //        String s = service.queryAi("Pitäskö kaivaa kaljat kaapista?");
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

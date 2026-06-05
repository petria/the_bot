package org.freakz.engine.services.generated;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.UnflaggedOption;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.services.api.ServiceRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_CHANNEL;

class GeneratedPageServiceSupportTest {

  @Test
  void resolvesPrivateChannelsToDefaultGluggaChannel() throws Exception {
    ServiceRequest request =
        ServiceRequest.builder()
            .engineRequest(EngineRequest.builder()
                .isPrivateChannel(true)
                .replyTo("#Ignored")
                .build())
            .results(results(""))
            .build();

    assertThat(GeneratedPageServiceSupport.resolveChannel(request)).isEqualTo("#amigafin");
  }

  @Test
  void resolvesExplicitChannelAsLowercase() throws Exception {
    ServiceRequest request =
        ServiceRequest.builder()
            .engineRequest(EngineRequest.builder()
                .isPrivateChannel(false)
                .replyTo("#Fallback")
                .build())
            .results(results("#HokanDEV"))
            .build();

    assertThat(GeneratedPageServiceSupport.resolveChannel(request)).isEqualTo("#hokandev");
  }

  @Test
  void parsesCountsAndBuildsGeneratedUrls() {
    assertThat(GeneratedPageServiceSupport.parseCount("42")).isEqualTo(42);
    assertThat(GeneratedPageServiceSupport.parseCount("bad")).isZero();
    assertThat(GeneratedPageServiceSupport.buildUrl("http://bot-web.local/", "id1", "token1"))
        .isEqualTo("http://bot-web.local/generated/id1?token=token1");
    assertThat(GeneratedPageServiceSupport.buildUrl(null, "id1", "token1"))
        .isEqualTo("http://localhost:8091/generated/id1?token=token1");
  }

  private com.martiansoftware.jsap.JSAPResult results(String args) throws Exception {
    JSAP jsap = new JSAP();
    jsap.registerParameter(new UnflaggedOption(ARG_CHANNEL).setRequired(false).setGreedy(false));
    return jsap.parse(args);
  }
}

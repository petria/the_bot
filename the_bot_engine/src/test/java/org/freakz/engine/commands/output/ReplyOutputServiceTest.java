package org.freakz.engine.commands.output;

import org.freakz.common.model.engine.EngineRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReplyOutputServiceTest {

  private final ReplyOutputService service = new ReplyOutputService();

  @Test
  void keepsNonIrcMultilineOutputReadable() {
    EngineRequest request = request("discord", false);

    String output = service.formatReply(request, "line one\nline two\nline three");

    assertThat(output).isEqualTo("line one\nline two\nline three");
  }

  @Test
  void limitsIrcPublicOutputToDefaultLineCap() {
    EngineRequest request = request("irc", false);

    String output = service.formatReply(request, "one\ntwo\nthree\nfour\nfive\nsix");

    assertThat(output.lines().count()).isEqualTo(4);
    assertThat(output).endsWith("(output shortened)");
  }

  @Test
  void allowsMoreIrcPrivateOutputLines() {
    EngineRequest request = request("irc", true);

    String output = service.formatReply(request, "one\ntwo\nthree\nfour\nfive\nsix");

    assertThat(output.lines().count()).isEqualTo(6);
    assertThat(output).doesNotContain("(output shortened)");
  }

  @Test
  void wrapsAndShortensLongIrcLine() {
    EngineRequest request = request("irc", false);
    String longWord = "x".repeat(1600);

    String output = service.formatReply(request, longWord);

    assertThat(output.lines().count()).isEqualTo(4);
    assertThat(output).endsWith("(output shortened)");
    assertThat(output.lines()).allMatch(line -> line.length() <= 380);
  }

  @Test
  void compactsIrcListsBeforeApplyingLineCap() {
    EngineRequest request = request("irc", false);

    String output = service.formatList(
        request,
        "HELP:",
        List.of("ping", "help aliases: !commands", "weather aliases: !saa, !sää", "topcounts[A] aliases: !topgl"),
        "Use !help commandName for details.");

    assertThat(output.lines().count()).isLessThanOrEqualTo(4);
    assertThat(output).contains("HELP: ping, help aliases: !commands");
    assertThat(output).contains("Use !help commandName for details.");
  }

  private EngineRequest request(String protocol, boolean privateChannel) {
    return EngineRequest.builder()
        .chatProtocol(protocol)
        .isPrivateChannel(privateChannel)
        .network(protocol.toUpperCase())
        .build();
  }
}

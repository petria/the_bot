package org.freakz.cli.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CliServiceTest {

  @Test
  void resolvesTargetFromFirstArgument() {
    CliService service = new CliService();

    assertThat(service.resolveTarget("debbox.local")).isEqualTo("debbox.local");
  }

  @Test
  void rejectsMissingTarget() {
    CliService service = new CliService();

    assertThatThrownBy(service::resolveTarget)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("<host>");
  }

  @Test
  void resolvesOneShotMessageFromRemainingArguments() {
    CliService service = new CliService();

    assertThat(service.resolveOneShotMessage("debbox.local", "!ping", "hello"))
        .isEqualTo("!ping hello");
    assertThat(service.resolveOneShotMessage("debbox.local")).isNull();
  }
}

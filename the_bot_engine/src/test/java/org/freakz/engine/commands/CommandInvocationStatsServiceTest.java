package org.freakz.engine.commands;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.freakz.engine.commands.handlers.PingCmd;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CommandInvocationStatsServiceTest {

  @Test
  void recordsCommandAndProviderCounts() {
    CommandInvocationStatsService service = new CommandInvocationStatsService((io.micrometer.core.instrument.MeterRegistry) null);
    HandlerClass handlerClass = pingHandler();

    service.recordInvocation(handlerClass);
    service.recordInvocation(handlerClass);

    assertThat(service.getCommandInvocationCount("main::ping")).isEqualTo(2);
    assertThat(service.getProviderInvocationCount("main")).isEqualTo(2);
    assertThat(service.getCommandInvocationCount("main::unknown")).isZero();
  }

  @Test
  void recordsMicrometerCounterWhenRegistryExists() {
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    CommandInvocationStatsService service = new CommandInvocationStatsService(registry);
    HandlerClass handlerClass = pingHandler();

    service.recordInvocation(handlerClass);

    assertThat(registry.get("thebot.command.invocations")
        .tag("provider", "main")
        .tag("command", "ping")
        .tag("class", PingCmd.class.getName())
        .counter()
        .count()).isEqualTo(1);
  }

  private HandlerClass pingHandler() {
    return HandlerClass.builder()
        .namespace("main")
        .commandName("ping")
        .clazz(PingCmd.class)
        .build();
  }
}

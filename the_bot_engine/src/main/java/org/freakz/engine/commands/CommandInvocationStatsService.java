package org.freakz.engine.commands;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;

@Service
public class CommandInvocationStatsService {

  private final ConcurrentMap<String, LongAdder> commandCounts = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, LongAdder> providerCounts = new ConcurrentHashMap<>();
  private final MeterRegistry meterRegistry;

  @Autowired
  public CommandInvocationStatsService(ObjectProvider<MeterRegistry> meterRegistryProvider) {
    this.meterRegistry = meterRegistryProvider.getIfAvailable();
  }

  CommandInvocationStatsService(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  public void recordInvocation(HandlerClass handlerClass) {
    if (handlerClass == null) {
      return;
    }

    String provider = normalize(handlerClass.getNamespace());
    String command = normalize(handlerClass.getCommandName());
    String canonicalName = handlerClass.getCanonicalName();

    commandCounts.computeIfAbsent(canonicalName, ignored -> new LongAdder()).increment();
    providerCounts.computeIfAbsent(provider, ignored -> new LongAdder()).increment();

    if (meterRegistry != null) {
      meterRegistry.counter(
          "thebot.command.invocations",
          "provider", provider,
          "command", command,
          "class", handlerClass.getClazz().getName()
      ).increment();
    }
  }

  public long getCommandInvocationCount(String canonicalName) {
    LongAdder count = commandCounts.get(canonicalName);
    return count == null ? 0 : count.sum();
  }

  public long getProviderInvocationCount(String namespace) {
    LongAdder count = providerCounts.get(normalize(namespace));
    return count == null ? 0 : count.sum();
  }

  private String normalize(String value) {
    return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
  }
}

package org.freakz.engine.commands;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;

@Service
public class CommandInvocationStatsService {

  private final ConcurrentMap<String, LongAdder> commandCounts = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, LongAdder> providerCounts = new ConcurrentHashMap<>();
  private final MeterRegistry meterRegistry;
  private final CommandStatsPersistenceService persistenceService;
  private volatile boolean dirty;
  
  @Autowired
  public CommandInvocationStatsService(ObjectProvider<MeterRegistry> meterRegistryProvider,
                                       CommandStatsPersistenceService persistenceService) {
    this.meterRegistry = meterRegistryProvider.getIfAvailable();
    this.persistenceService = persistenceService;
    loadStatsFromPersistence();
  }
  
  /**
   * Constructor for testing
   */
  CommandInvocationStatsService(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
    this.persistenceService = null;
  }

  CommandInvocationStatsService(MeterRegistry meterRegistry, CommandStatsPersistenceService persistenceService) {
    this.meterRegistry = meterRegistry;
    this.persistenceService = persistenceService;
    loadStatsFromPersistence();
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
    dirty = true;
  }

  public void recordDynamicInvocation(String namespace, String commandName) {
    String provider = normalize(namespace);
    String command = normalize(commandName);
    String canonicalName = provider + "::" + command;

    commandCounts.computeIfAbsent(canonicalName, ignored -> new LongAdder()).increment();
    providerCounts.computeIfAbsent(provider, ignored -> new LongAdder()).increment();

    if (meterRegistry != null) {
      meterRegistry.counter(
          "thebot.command.invocations",
          "provider", provider,
          "command", command,
          "class", "dynamic"
      ).increment();
    }
    dirty = true;
  }

  private boolean saveStatsToPersistence() {
    if (persistenceService != null) {
      try {
        Map<String, Long> statsMap = new HashMap<>();
        for (Map.Entry<String, LongAdder> entry : commandCounts.entrySet()) {
          statsMap.put(entry.getKey(), entry.getValue().sum());
        }
        persistenceService.saveStats(statsMap);
        return true;
      } catch (Exception e) {
        // Don't let persistence issues crash the application
        // Just log and continue
      }
    }
    return false;
  }

  private void loadStatsFromPersistence() {
    if (persistenceService != null) {
      try {
        Map<String, Long> loadedStats = persistenceService.loadStats();
        for (Map.Entry<String, Long> entry : loadedStats.entrySet()) {
          String canonicalName = entry.getKey();
          long count = entry.getValue() == null ? 0 : entry.getValue();
          commandCounts.computeIfAbsent(canonicalName, ignored -> new LongAdder()).add(count);
          providerCounts.computeIfAbsent(providerFromCanonicalName(canonicalName), ignored -> new LongAdder()).add(count);
        }
      } catch (Exception e) {
        // Don't let persistence issues crash the application
        // Just continue with empty stats
      }
    }
  }

  @PreDestroy
  public void shutdown() {
    if (persistenceService != null) {
      try {
        saveStatsToPersistence();
      } catch (Exception e) {
        // Don't let persistence shutdown issues crash the application
      }
    }
  }

  @Scheduled(fixedRate = 10000)
  public void saveStatsPeriodically() {
    if (!dirty || persistenceService == null) {
      return;
    }
    if (saveStatsToPersistence()) {
      dirty = false;
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

  private String providerFromCanonicalName(String canonicalName) {
    if (canonicalName == null) {
      return "";
    }
    int separator = canonicalName.indexOf("::");
    return separator < 0 ? "" : normalize(canonicalName.substring(0, separator));
  }
}

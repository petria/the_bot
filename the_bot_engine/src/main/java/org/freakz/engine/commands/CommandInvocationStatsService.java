package org.freakz.engine.commands;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;

import jakarta.annotation.PreDestroy;
import org.freakz.engine.config.ConfigService;

@Service
public class CommandInvocationStatsService implements ApplicationListener<ContextRefreshedEvent> {

  private final ConcurrentMap<String, LongAdder> commandCounts = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, LongAdder> providerCounts = new ConcurrentHashMap<>();
  private final MeterRegistry meterRegistry;
  private final CommandStatsPersistenceService persistenceService;
  private final ConfigService configService;
  
  // Add a flag to track if startup has been called
  private boolean isStarted = false;
  
  // Flag to indicate when stats were last saved (for periodic saving)
  private volatile long lastSaveTime = 0;
  
  @Autowired
  public CommandInvocationStatsService(ObjectProvider<MeterRegistry> meterRegistryProvider,
                                       CommandStatsPersistenceService persistenceService,
                                       ConfigService configService) {
    this.meterRegistry = meterRegistryProvider.getIfAvailable();
    this.persistenceService = persistenceService;
    this.configService = configService;
    // Load existing stats on initialization
    loadStatsFromPersistence();
  }
  
  /**
   * Constructor for testing
   */
  CommandInvocationStatsService(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
    this.persistenceService = null;
    this.configService = null;
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
    
    // Flag that stats need to be saved - we'll save periodically instead of each invocation
    // This is for efficiency, especially when there are rapid command usage patterns
    lastSaveTime = 0;
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
    
    // Flag that stats need to be saved - we'll save periodically instead of each invocation
    lastSaveTime = 0;
  }

  private void saveStatsToPersistence() {
    if (persistenceService != null) {
      try {
        Map<String, Long> statsMap = new HashMap<>();
        for (Map.Entry<String, LongAdder> entry : commandCounts.entrySet()) {
          statsMap.put(entry.getKey(), entry.getValue().sum());
        }
        persistenceService.saveStats(statsMap);
      } catch (Exception e) {
        // Don't let persistence issues crash the application
        // Just log and continue
      }
    }
  }

  private void loadStatsFromPersistence() {
    if (persistenceService != null) {
      try {
        Map<String, Long> loadedStats = persistenceService.loadStats();
        for (Map.Entry<String, Long> entry : loadedStats.entrySet()) {
          commandCounts.computeIfAbsent(entry.getKey(), ignored -> new LongAdder()).add(entry.getValue());
        }
      } catch (Exception e) {
        // Don't let persistence issues crash the application
        // Just continue with empty stats
      }
    }
  }

  @Override
  public void onApplicationEvent(ContextRefreshedEvent event) {
    if (!isStarted && persistenceService != null) {
      try {
        // Set the data directory for the persistence service, but only after it's been created and 
        // we have access to the data directory value from configuration  
        if (persistenceService instanceof JsonCommandStatsPersistenceService) {
          // Use the standard pattern from other code: configService.getRuntimeDataFile("command_stats.json")
          ((JsonCommandStatsPersistenceService) persistenceService).setDataDirectory(
            configService.getRuntimeDataFile("command_stats.json").getParent()
          );
        }
        persistenceService.startup();
        isStarted = true;
      } catch (Exception e) {
        // Don't let persistence startup issues crash the application
      }
    }
  }

  @PreDestroy
  public void shutdown() {
    if (persistenceService != null) {
      try {
        // Save any final stats on shutdown
        Map<String, Long> statsMap = new HashMap<>();
        for (Map.Entry<String, LongAdder> entry : commandCounts.entrySet()) {
          statsMap.put(entry.getKey(), entry.getValue().sum());
        }
        persistenceService.saveStats(statsMap);
        persistenceService.shutdown();
      } catch (Exception e) {
        // Don't let persistence shutdown issues crash the application
      }
    }
  }

  // Periodically save command stats - execute every 60 seconds
  @Scheduled(fixedRate = 60000)
  public void saveStatsPeriodically() {
    if (lastSaveTime > 0 || persistenceService == null) {
      return; // No need to save if it was saved recently or no persistence service
    }
    
    saveStatsToPersistence();
    lastSaveTime = System.currentTimeMillis();
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

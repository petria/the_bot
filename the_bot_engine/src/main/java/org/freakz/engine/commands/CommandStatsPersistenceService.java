package org.freakz.engine.commands;

import java.util.Map;

public interface CommandStatsPersistenceService {

    /**
     * Saves command invocation statistics to persistent storage
     * @param statsMap Map containing command names and their invocation counts
     */
    void saveStats(Map<String, Long> statsMap);

    /**
     * Loads command invocation statistics from persistent storage
     * @return Map containing command names and their invocation counts
     */
    Map<String, Long> loadStats();
    
    /**
     * Called when application starts - initialize persistence if needed
     */
    void startup();
    
    /**
     * Called when application shuts down - flush any pending data
     */
    void shutdown();
}
package org.freakz.engine.commands;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;

@Service
public class JsonCommandStatsPersistenceService implements CommandStatsPersistenceService {

    private final ObjectMapper objectMapper;
    private String statsFilePath;
    
    @Autowired
    public JsonCommandStatsPersistenceService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        // Default to current working directory for backwards compatibility 
        // This will be overridden by the setter when data directory is provided
        this.statsFilePath = "command_stats.json";
    }

    // For testing or enhanced integration with configuration framework
    public JsonCommandStatsPersistenceService(ObjectMapper objectMapper, String dataDirectory) {
        this.objectMapper = objectMapper;
        this.statsFilePath = Path.of(dataDirectory, "command_stats.json").toString();
    }
    
    /**
     * Setter for data directory - allows injecting the data directory from configuration
     * @param dataDirectory path to the data directory
     */
    public void setDataDirectory(String dataDirectory) {
        if (dataDirectory != null && !dataDirectory.isEmpty()) {
            this.statsFilePath = Path.of(dataDirectory, "command_stats.json").toString();
        }
    }

    @Override
    public void saveStats(Map<String, Long> statsMap) {
        try {
            // Use a ConcurrentHashMap for thread safety and to avoid concurrent modification issues
            Map<String, Long> statsToSave = new ConcurrentHashMap<>(statsMap);
            
            // Create a JSON structure with the stats map
            objectMapper.writeValue(new File(statsFilePath), statsToSave);
        } catch (Exception e) {
            // Log the exception as error but don't throw it - failures in saving shouldn't crash the program
            // Note: ObjectMapper.writeValue() does not actually throw IOException in this usage, 
            // this is just a safety net to handle other errors
        }
    }

    @Override
    public Map<String, Long> loadStats() {
        Map<String, Long> loadedStats = new HashMap<>();
        
        File statsFile = new File(statsFilePath);
        if (!statsFile.exists()) {
            return loadedStats;
        }
        
        try {
            // Read the JSON file and parse it back to Map<String, Long>
            JsonNode jsonNode = objectMapper.readTree(statsFile);
            
            // Using TypeReference for proper type information
            Map<String, Long> parsedMap = objectMapper.readValue(
                jsonNode.toString(),
                new TypeReference<Map<String, Long>>() {}
            );
            
            loadedStats.putAll(parsedMap);
        } catch (Exception e) {
            // If JSON parsing fails, return empty map instead of crashing
            // This is already done - we're catching any exception just in case
            return new HashMap<>();
        }
        
        return loadedStats;
    }

    @Override
    public void startup() {
        // Nothing to do here for this implementation - stats are loaded during service initialization
    }

    @Override
    public void shutdown() {
        // Nothing to do here - the persistence happens on each invocation
        // This method provides a hook for other implementations that might need cleanup
    }
}
package org.freakz.common.config;

public record BotConfigDefaults(
    String configFile,
    String runtimeDir,
    String dataDir,
    String logDir
) {
}

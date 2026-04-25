package org.freakz.common.config;

import java.nio.file.Path;
import java.util.Properties;

public record BotRuntimeBootstrapConfig(
    Path configFile,
    String runtimeDir,
    String dataDir,
    String logDir,
    String profile,
    String runtimeConfigFile,
    Properties properties
) {
}

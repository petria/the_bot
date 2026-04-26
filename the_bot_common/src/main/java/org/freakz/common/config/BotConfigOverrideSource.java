package org.freakz.common.config;

import java.util.Optional;

@FunctionalInterface
public interface BotConfigOverrideSource {

  BotConfigOverrideSource NONE = propertyKey -> Optional.empty();

  Optional<String> findOverride(String propertyKey);
}

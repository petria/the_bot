package org.freakz.engine.services.notifications;

import org.freakz.common.model.engine.notify.UserNotifyRule;
import org.freakz.common.model.engine.notify.UserNotifyRuleConfig;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

class UserNotifyRuleStore {

  static final String USER_NOTIFY_RULES_FILE = "user-notify-rules.json";
  static final int MAX_COOLDOWN_SECONDS = 86_400;

  private final Path rulesFile;
  private final JsonMapper jsonMapper;
  private UserNotifyRuleConfig config = new UserNotifyRuleConfig();

  UserNotifyRuleStore(Path rulesFile, JsonMapper jsonMapper) {
    this.rulesFile = Objects.requireNonNull(rulesFile, "rulesFile");
    this.jsonMapper = Objects.requireNonNull(jsonMapper, "jsonMapper");
    reload();
  }

  synchronized UserNotifyRuleConfig reload() {
    if (!Files.exists(rulesFile)) {
      config = new UserNotifyRuleConfig();
      return config;
    }
    try {
      config = normalize(jsonMapper.readValue(rulesFile.toFile(), UserNotifyRuleConfig.class), false);
      return copyConfig(config);
    } catch (RuntimeException e) {
      throw new IllegalStateException("Could not read user notify rules: " + rulesFile.toAbsolutePath(), e);
    }
  }

  synchronized List<UserNotifyRule> findByUsername(String username) {
    String normalizedUsername = normalizeText(username);
    if (normalizedUsername == null) {
      return List.of();
    }
    return config.getRules().stream()
        .filter(rule -> normalizedUsername.equals(normalizeText(rule.getUsername())))
        .map(this::copyRule)
        .toList();
  }

  synchronized List<UserNotifyRule> findEnabled() {
    return config.getRules().stream()
        .filter(UserNotifyRule::isEnabled)
        .map(this::copyRule)
        .toList();
  }

  synchronized UserNotifyRule create(String username, UserNotifyRule incoming) {
    long now = System.currentTimeMillis();
    UserNotifyRule rule = copyRule(incoming == null ? new UserNotifyRule() : incoming);
    rule.setId(UUID.randomUUID().toString());
    rule.setUsername(username);
    rule.setCreatedAt(now);
    rule.setUpdatedAt(now);
    UserNotifyRule normalized = normalizeRule(rule, true);
    List<UserNotifyRule> rules = new ArrayList<>(config.getRules());
    rules.add(normalized);
    save(new UserNotifyRuleConfig(rules));
    return copyRule(normalized);
  }

  synchronized UserNotifyRule update(String username, String id, UserNotifyRule incoming) {
    String normalizedUsername = requireUsername(username);
    String normalizedId = requireText(id, "Rule id is required");
    UserNotifyRule existing = config.getRules().stream()
        .filter(rule -> normalizedId.equals(rule.getId()))
        .filter(rule -> normalizedUsername.equals(normalizeText(rule.getUsername())))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Notify rule not found: " + id));
    UserNotifyRule rule = copyRule(incoming == null ? new UserNotifyRule() : incoming);
    rule.setId(existing.getId());
    rule.setUsername(existing.getUsername());
    rule.setCreatedAt(existing.getCreatedAt());
    rule.setUpdatedAt(System.currentTimeMillis());
    UserNotifyRule normalized = normalizeRule(rule, true);
    List<UserNotifyRule> rules = config.getRules().stream()
        .map(current -> normalizedId.equals(current.getId()) ? normalized : current)
        .toList();
    save(new UserNotifyRuleConfig(rules));
    return copyRule(normalized);
  }

  synchronized void delete(String username, String id) {
    String normalizedUsername = requireUsername(username);
    String normalizedId = requireText(id, "Rule id is required");
    int before = config.getRules().size();
    List<UserNotifyRule> rules = config.getRules().stream()
        .filter(rule -> !(normalizedId.equals(rule.getId()) && normalizedUsername.equals(normalizeText(rule.getUsername()))))
        .toList();
    if (rules.size() == before) {
      throw new IllegalArgumentException("Notify rule not found: " + id);
    }
    save(new UserNotifyRuleConfig(rules));
  }

  private synchronized void save(UserNotifyRuleConfig incoming) {
    UserNotifyRuleConfig normalized = normalize(incoming, true);
    try {
      Path parent = rulesFile.toAbsolutePath().getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      String json = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(normalized);
      Path tempFile = Files.createTempFile(parent, rulesFile.getFileName().toString(), ".tmp");
      Files.writeString(tempFile, json, Charset.defaultCharset());
      Files.move(tempFile, rulesFile, StandardCopyOption.REPLACE_EXISTING);
      config = normalized;
    } catch (IOException | RuntimeException e) {
      throw new IllegalStateException("Could not write user notify rules: " + rulesFile.toAbsolutePath(), e);
    }
  }

  private UserNotifyRuleConfig normalize(UserNotifyRuleConfig incoming, boolean validate) {
    UserNotifyRuleConfig source = incoming == null ? new UserNotifyRuleConfig() : incoming;
    List<UserNotifyRule> rules = new ArrayList<>();
    for (UserNotifyRule rule : source.getRules()) {
      if (rule != null) {
        rules.add(normalizeRule(rule, validate));
      }
    }
    return new UserNotifyRuleConfig(rules);
  }

  private UserNotifyRule normalizeRule(UserNotifyRule source, boolean validate) {
    UserNotifyRule rule = copyRule(source);
    rule.setId(clean(rule.getId()));
    rule.setUsername(clean(rule.getUsername()));
    rule.setSourceEchoToAlias(clean(rule.getSourceEchoToAlias()));
    rule.setSourceDisplayName(clean(rule.getSourceDisplayName()));
    rule.setPatternType(normalizePatternType(rule.getPatternType()));
    rule.setPattern(clean(rule.getPattern()));
    rule.setDestinationConnectionType(normalizeConnectionType(rule.getDestinationConnectionType()));
    rule.setCooldownSeconds(normalizeCooldown(rule.getCooldownSeconds()));
    if (validate) {
      requireText(rule.getId(), "Rule id is required");
      requireUsername(rule.getUsername());
      requireText(rule.getSourceEchoToAlias(), "Source channel is required");
      if (UserNotifyRule.PATTERN_TYPE_REGEX.equals(rule.getPatternType())) {
        requireText(rule.getPattern(), "Regex pattern is required");
        try {
          Pattern.compile(rule.getPattern());
        } catch (PatternSyntaxException e) {
          throw new IllegalArgumentException("Invalid regex pattern: " + e.getDescription(), e);
        }
      }
      requireText(rule.getDestinationConnectionType(), "Destination connection type is required");
    }
    return rule;
  }

  private String normalizePatternType(String patternType) {
    String normalized = clean(patternType);
    if (normalized == null) {
      return UserNotifyRule.PATTERN_TYPE_PRESET_MENTION;
    }
    normalized = normalized.toUpperCase(Locale.ROOT);
    if (UserNotifyRule.PATTERN_TYPE_REGEX.equals(normalized)) {
      return normalized;
    }
    return UserNotifyRule.PATTERN_TYPE_PRESET_MENTION;
  }

  private String normalizeConnectionType(String connectionType) {
    String normalized = clean(connectionType);
    return normalized == null ? UserNotifyRule.DEFAULT_DESTINATION_CONNECTION_TYPE : normalized.toUpperCase(Locale.ROOT);
  }

  private int normalizeCooldown(int cooldownSeconds) {
    if (cooldownSeconds < 0) {
      return 0;
    }
    if (cooldownSeconds > MAX_COOLDOWN_SECONDS) {
      return MAX_COOLDOWN_SECONDS;
    }
    return cooldownSeconds == 0 ? UserNotifyRule.DEFAULT_COOLDOWN_SECONDS : cooldownSeconds;
  }

  private String requireUsername(String username) {
    return requireText(username, "Username is required");
  }

  private String requireText(String value, String message) {
    String cleaned = clean(value);
    if (cleaned == null) {
      throw new IllegalArgumentException(message);
    }
    return cleaned;
  }

  private String clean(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private String normalizeText(String value) {
    String cleaned = clean(value);
    return cleaned == null ? null : cleaned.toLowerCase(Locale.ROOT);
  }

  private UserNotifyRuleConfig copyConfig(UserNotifyRuleConfig source) {
    return new UserNotifyRuleConfig(source.getRules().stream().map(this::copyRule).toList());
  }

  private UserNotifyRule copyRule(UserNotifyRule source) {
    return new UserNotifyRule(
        source.getId(),
        source.getUsername(),
        source.isEnabled(),
        source.getSourceEchoToAlias(),
        source.getSourceDisplayName(),
        source.getPatternType(),
        source.getPattern(),
        source.getDestinationConnectionType(),
        source.getCooldownSeconds(),
        source.getCreatedAt(),
        source.getUpdatedAt());
  }
}

package org.freakz.common.model.engine.aicommand;

import java.util.ArrayList;
import java.util.List;

public class AiCommandDefinition {

  public static final String TOOL_RESULT_MODE_FORMATTED_TEXT = "FORMATTED_TEXT";
  public static final String TOOL_RESULT_MODE_MODEL_FINAL = "MODEL_FINAL";

  private String name;
  private boolean enabled;
  private String description;
  private String usage;
  private List<String> aliases = new ArrayList<>();
  private String requiredPermission;
  private String instructions;
  private List<String> allowedTools = new ArrayList<>();
  private int maxToolIterations;
  private String toolResultMode;

  public AiCommandDefinition() {
  }

  public AiCommandDefinition(
      String name,
      boolean enabled,
      String description,
      String usage,
      List<String> aliases,
      String requiredPermission,
      String instructions,
      List<String> allowedTools,
      int maxToolIterations) {
    this(
        name,
        enabled,
        description,
        usage,
        aliases,
        requiredPermission,
        instructions,
        allowedTools,
        maxToolIterations,
        null);
  }

  public AiCommandDefinition(
      String name,
      boolean enabled,
      String description,
      String usage,
      List<String> aliases,
      String requiredPermission,
      String instructions,
      List<String> allowedTools,
      int maxToolIterations,
      String toolResultMode) {
    this.name = name;
    this.enabled = enabled;
    this.description = description;
    this.usage = usage;
    this.aliases = aliases == null ? new ArrayList<>() : new ArrayList<>(aliases);
    this.requiredPermission = requiredPermission;
    this.instructions = instructions;
    this.allowedTools = allowedTools == null ? new ArrayList<>() : new ArrayList<>(allowedTools);
    this.maxToolIterations = maxToolIterations;
    this.toolResultMode = toolResultMode;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getUsage() {
    return usage;
  }

  public void setUsage(String usage) {
    this.usage = usage;
  }

  public List<String> getAliases() {
    return aliases;
  }

  public void setAliases(List<String> aliases) {
    this.aliases = aliases == null ? new ArrayList<>() : new ArrayList<>(aliases);
  }

  public String getRequiredPermission() {
    return requiredPermission;
  }

  public void setRequiredPermission(String requiredPermission) {
    this.requiredPermission = requiredPermission;
  }

  public String getInstructions() {
    return instructions;
  }

  public void setInstructions(String instructions) {
    this.instructions = instructions;
  }

  public List<String> getAllowedTools() {
    return allowedTools;
  }

  public void setAllowedTools(List<String> allowedTools) {
    this.allowedTools = allowedTools == null ? new ArrayList<>() : new ArrayList<>(allowedTools);
  }

  public int getMaxToolIterations() {
    return maxToolIterations;
  }

  public void setMaxToolIterations(int maxToolIterations) {
    this.maxToolIterations = maxToolIterations;
  }

  public String getToolResultMode() {
    return toolResultMode;
  }

  public void setToolResultMode(String toolResultMode) {
    this.toolResultMode = toolResultMode;
  }
}

package org.freakz.engine.commands.ai;

import org.freakz.common.model.engine.aicommand.AiCommandDefinition;

import java.util.List;

public final class AiCommandHelpFormatter {

  private AiCommandHelpFormatter() {
  }

  public static String formatDetailed(AiCommandDefinition command) {
    StringBuilder sb = new StringBuilder();
    sb.append("Usage    : ");
    sb.append(formatUsage(command));
    sb.append("\n");

    String aliases = formatAliases(command);
    if (!aliases.isBlank()) {
      sb.append("Aliases  : ");
      sb.append(aliases);
      sb.append("\n");
    }

    sb.append("Help     : ");
    sb.append(formatHelp(command));
    sb.append("\n");
    return sb.toString();
  }

  public static String formatUsage(AiCommandDefinition command) {
    if (command == null) {
      return "";
    }
    String usage = command.getUsage();
    if (usage == null || usage.isBlank()) {
      String name = command.getName() == null ? "" : command.getName().trim().toLowerCase();
      return name.isBlank() ? "" : "!" + name;
    }
    return usage.trim();
  }

  public static String formatAliases(AiCommandDefinition command) {
    if (command == null || command.getAliases() == null || command.getAliases().isEmpty()) {
      return "";
    }
    List<String> aliases = command.getAliases().stream()
        .filter(alias -> alias != null && !alias.isBlank())
        .sorted(String.CASE_INSENSITIVE_ORDER)
        .toList();
    return String.join(", ", aliases);
  }

  public static String formatHelp(AiCommandDefinition command) {
    if (command == null) {
      return "";
    }
    String description = command.getDescription() == null ? "" : command.getDescription().trim();
    if (!command.isEnabled()) {
      if (description.isBlank()) {
        return "Disabled.";
      }
      return "Disabled. " + description;
    }
    return description.isBlank() ? "Runtime Hermes-backed AI command." : description;
  }
}

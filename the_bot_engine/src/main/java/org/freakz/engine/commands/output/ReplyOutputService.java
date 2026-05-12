package org.freakz.engine.commands.output;

import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.config.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class ReplyOutputService {

  private static final int DEFAULT_IRC_MAX_LINE_CHARS = 380;
  private static final int DEFAULT_IRC_MAX_PUBLIC_LINES = 4;
  private static final int DEFAULT_IRC_MAX_PRIVATE_LINES = 8;
  private static final String SHORTENED_SUFFIX = " (output shortened)";

  private final ConfigService configService;

  @Autowired
  public ReplyOutputService(ConfigService configService) {
    this.configService = configService;
  }

  ReplyOutputService() {
    this.configService = null;
  }

  public String formatReply(EngineRequest request, String reply) {
    if (reply == null) {
      return null;
    }

    String normalized = normalize(reply);
    if (!isIrc(request)) {
      return normalized;
    }

    return limitIrcOutput(request, normalized);
  }

  public String formatList(EngineRequest request, String title, List<String> entries, String footer) {
    if (!isIrc(request)) {
      return formatMultilineList(title, entries, footer);
    }
    return limitIrcOutput(request, formatCompactList(request, title, entries, footer));
  }

  public boolean isIrc(EngineRequest request) {
    if (request == null) {
      return false;
    }
    String protocol = request.getChatProtocol();
    if (protocol != null && protocol.equalsIgnoreCase("irc")) {
      return true;
    }
    String network = request.getNetwork();
    return network != null && network.toLowerCase(Locale.ROOT).contains("irc");
  }

  private String formatMultilineList(String title, List<String> entries, String footer) {
    StringBuilder sb = new StringBuilder();
    if (title != null && !title.isBlank()) {
      sb.append(title.trim()).append("\n");
    }
    for (String entry : entries) {
      if (entry != null && !entry.isBlank()) {
        sb.append(entry.trim()).append("\n");
      }
    }
    if (footer != null && !footer.isBlank()) {
      sb.append(footer.trim()).append("\n");
    }
    return normalize(sb.toString());
  }

  private String formatCompactList(EngineRequest request, String title, List<String> entries, String footer) {
    int maxLineChars = getIrcMaxLineChars();
    List<String> lines = new ArrayList<>();
    String current = title == null ? "" : title.trim();

    for (String entry : entries) {
      if (entry == null || entry.isBlank()) {
        continue;
      }
      String cleanEntry = entry.trim();
      String separator = current.isBlank() ? "" : current.endsWith(":") ? " " : ", ";
      if (!current.isBlank() && current.length() + separator.length() + cleanEntry.length() > maxLineChars) {
        lines.add(current);
        current = cleanEntry;
      } else {
        current += separator + cleanEntry;
      }
    }
    if (!current.isBlank()) {
      lines.add(current);
    }
    if (footer != null && !footer.isBlank()) {
      lines.add(footer.trim());
    }
    return String.join("\n", lines);
  }

  private String limitIrcOutput(EngineRequest request, String reply) {
    int maxLineChars = getIrcMaxLineChars();
    int maxLines = getIrcMaxLines(request);
    List<String> outputLines = new ArrayList<>();
    boolean shortened = false;

    for (String line : reply.split("\\n", -1)) {
      String trimmed = line.trim();
      if (trimmed.isBlank()) {
        continue;
      }
      List<String> wrapped = wrapLine(trimmed, maxLineChars);
      for (String wrappedLine : wrapped) {
        if (outputLines.size() >= maxLines) {
          shortened = true;
          break;
        }
        outputLines.add(wrappedLine);
      }
      if (shortened) {
        break;
      }
    }

    if (outputLines.isEmpty()) {
      return "";
    }
    if (shortened) {
      int lastIndex = outputLines.size() - 1;
      outputLines.set(lastIndex, appendShortenedSuffix(outputLines.get(lastIndex), maxLineChars));
    }
    return String.join("\n", outputLines);
  }

  private List<String> wrapLine(String line, int maxLineChars) {
    List<String> lines = new ArrayList<>();
    String remaining = line;
    while (remaining.length() > maxLineChars) {
      int splitAt = remaining.lastIndexOf(' ', maxLineChars);
      if (splitAt < maxLineChars / 2) {
        splitAt = maxLineChars;
      }
      lines.add(remaining.substring(0, splitAt).trim());
      remaining = remaining.substring(splitAt).trim();
    }
    if (!remaining.isBlank()) {
      lines.add(remaining);
    }
    return lines;
  }

  private String appendShortenedSuffix(String line, int maxLineChars) {
    int allowed = Math.max(0, maxLineChars - SHORTENED_SUFFIX.length());
    String trimmed = line.length() > allowed ? line.substring(0, allowed).trim() : line;
    return trimmed + SHORTENED_SUFFIX;
  }

  private String normalize(String text) {
    String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
    StringBuilder sb = new StringBuilder();
    boolean previousBlank = false;
    for (String line : normalized.split("\\n", -1)) {
      String trimmed = line.stripTrailing();
      boolean blank = trimmed.isBlank();
      if (blank && previousBlank) {
        continue;
      }
      sb.append(trimmed).append("\n");
      previousBlank = blank;
    }
    return sb.toString().trim();
  }

  private int getIrcMaxLineChars() {
    return Math.max(80, getConfigInt("the.bot.output.irc.max-line-chars", "THE_BOT_OUTPUT_IRC_MAX_LINE_CHARS", DEFAULT_IRC_MAX_LINE_CHARS));
  }

  private int getIrcMaxLines(EngineRequest request) {
    if (request != null && request.isPrivateChannel()) {
      return Math.max(1, getConfigInt("the.bot.output.irc.max-private-lines", "THE_BOT_OUTPUT_IRC_MAX_PRIVATE_LINES", DEFAULT_IRC_MAX_PRIVATE_LINES));
    }
    return Math.max(1, getConfigInt("the.bot.output.irc.max-public-lines", "THE_BOT_OUTPUT_IRC_MAX_PUBLIC_LINES", DEFAULT_IRC_MAX_PUBLIC_LINES));
  }

  private int getConfigInt(String propertyKey, String envKey, int defaultValue) {
    if (configService == null) {
      return defaultValue;
    }
    return configService.getConfigIntValue(propertyKey, envKey, defaultValue);
  }
}

package org.freakz.engine.services.ai.claw;

import org.freakz.common.chat.ChatIdentityUtil;
import org.freakz.common.users.BotPermission;
import org.freakz.engine.config.ConfigService;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Service
public class OpenClawLogAccessService {

  private static final Pattern LOG_DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
  private static final int DEFAULT_LINES = 80;
  private static final int MAX_LINES = 500;

  private final ConfigService configService;
  private final HokanNodeContextTokenService tokenService;

  public OpenClawLogAccessService(ConfigService configService, HokanNodeContextTokenService tokenService) {
    this.configService = configService;
    this.tokenService = tokenService;
  }

  public LogReadResponse readLogs(LogReadRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("missing request");
    }

    HokanNodeContextTokenService.VerifiedNodeContext context =
        tokenService.verifyToken(request.hokanContextToken());
    LogTarget target = resolveTarget(context, request);
    verifyPermission(context, target);

    Path root = Path.of(configService.getBotLogDir()).toAbsolutePath().normalize();
    Path logDir = root
        .resolve(target.protocol())
        .resolve(target.network())
        .resolve(target.chatType())
        .resolve(target.chatTarget())
        .normalize();
    if (!logDir.startsWith(root)) {
      throw new IllegalArgumentException("invalid log target");
    }

    List<String> availableFiles = listLogFiles(logDir);
    String selectedDate = normalizeDate(request.date());
    if (selectedDate == null && !availableFiles.isEmpty()) {
      selectedDate = availableFiles.getFirst().replaceFirst("\\.log$", "");
    }
    if (selectedDate == null) {
      return new LogReadResponse(
          target.scope(),
          target.protocol(),
          target.network(),
          target.chatType(),
          target.chatTarget(),
          null,
          false,
          lines(request.lines()),
          availableFiles,
          "");
    }

    Path logFile = logDir.resolve(selectedDate + ".log").normalize();
    if (!logFile.startsWith(logDir)) {
      throw new IllegalArgumentException("invalid log date");
    }

    int lines = lines(request.lines());
    boolean found = Files.isRegularFile(logFile);
    String content = found ? tail(logFile, lines) : "";
    return new LogReadResponse(
        target.scope(),
        target.protocol(),
        target.network(),
        target.chatType(),
        target.chatTarget(),
        selectedDate,
        found,
        lines,
        availableFiles,
        content);
  }

  private LogTarget resolveTarget(
      HokanNodeContextTokenService.VerifiedNodeContext context,
      LogReadRequest request) {
    String scope = sanitizeScope(request.scope());
    String protocol = segment(firstNonBlank(request.protocol(), context.chatProtocol()), "chat");
    String network = segment(firstNonBlank(request.network(), context.network()), "unknown");
    String chatType = segment(firstNonBlank(request.chatType(), context.chatType()), "channel");
    String chatTarget = segment(firstNonBlank(request.chatTarget(), context.chatTarget()), "unknown");
    return new LogTarget(scope, protocol, network, chatType, chatTarget);
  }

  private void verifyPermission(
      HokanNodeContextTokenService.VerifiedNodeContext context,
      LogTarget target) {
    if (context.hasPermission(BotPermission.LOGS_READ_ALL)) {
      return;
    }

    boolean currentChat = same(target.protocol(), context.chatProtocol())
        && same(target.network(), context.network())
        && same(target.chatType(), context.chatType())
        && same(target.chatTarget(), context.chatTarget());
    boolean publicChannel = "channel".equals(target.chatType());
    boolean privateChat = isPrivateChat(target.chatType());

    if (currentChat && context.hasPermission(BotPermission.LOGS_READ_CURRENT_CHAT)) {
      return;
    }
    if (currentChat && publicChannel && context.hasPermission(BotPermission.LOGS_READ_CURRENT_CHANNEL)) {
      return;
    }
    if (currentChat && privateChat && context.hasPermission(BotPermission.LOGS_READ_CURRENT_USER_DM)) {
      return;
    }
    if (publicChannel && context.hasPermission(BotPermission.LOGS_READ_ALL_PUBLIC_CHANNELS)) {
      return;
    }
    if (privateChat && context.hasPermission(BotPermission.LOGS_READ_ALL_PRIVATE_CHATS)) {
      return;
    }

    throw new SecurityException("permission denied for log scope " + target.scope());
  }

  private List<String> listLogFiles(Path logDir) {
    if (!Files.isDirectory(logDir)) {
      return List.of();
    }
    try (Stream<Path> stream = Files.list(logDir)) {
      return stream
          .filter(Files::isRegularFile)
          .map(path -> path.getFileName().toString())
          .filter(name -> name.endsWith(".log"))
          .filter(name -> LOG_DATE.matcher(name.substring(0, name.length() - 4)).matches())
          .sorted(Comparator.reverseOrder())
          .limit(120)
          .toList();
    } catch (IOException e) {
      return List.of();
    }
  }

  private String tail(Path file, int lines) {
    ArrayDeque<String> buffer = new ArrayDeque<>(lines);
    try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
      String line;
      while ((line = reader.readLine()) != null) {
        if (buffer.size() == lines) {
          buffer.removeFirst();
        }
        buffer.addLast(line);
      }
    } catch (IOException e) {
      throw new IllegalStateException("failed to read log file", e);
    }
    return String.join("\n", buffer);
  }

  private String normalizeDate(String date) {
    if (date == null || date.isBlank()) {
      return null;
    }
    String normalized = date.trim();
    if (normalized.endsWith(".log")) {
      normalized = normalized.substring(0, normalized.length() - 4);
    }
    if (!LOG_DATE.matcher(normalized).matches()) {
      throw new IllegalArgumentException("invalid log date");
    }
    LocalDate.parse(normalized);
    return normalized;
  }

  private String sanitizeScope(String scope) {
    String value = scope == null || scope.isBlank() ? "current-chat" : scope.trim().toLowerCase();
    return switch (value) {
      case "current-chat", "current-channel", "current-user-dm", "all-public-channels", "all-private-chats", "all" -> value;
      default -> throw new IllegalArgumentException("invalid log scope");
    };
  }

  private int lines(Integer lines) {
    if (lines == null || lines < 1) {
      return DEFAULT_LINES;
    }
    return Math.min(lines, MAX_LINES);
  }

  private boolean same(String left, String right) {
    return segment(left, "unknown").equals(segment(right, "unknown"));
  }

  private boolean isPrivateChat(String chatType) {
    return "dm".equals(chatType) || "private".equals(chatType) || "private-channel".equals(chatType);
  }

  private String segment(String value, String fallback) {
    String sanitized = ChatIdentityUtil.sanitize(value, fallback);
    if (sanitized == null || sanitized.isBlank() || sanitized.contains("/") || sanitized.contains("\\") || sanitized.contains("..")) {
      throw new IllegalArgumentException("invalid log target segment");
    }
    return sanitized;
  }

  private String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value;
      }
    }
    return null;
  }

  private record LogTarget(String scope, String protocol, String network, String chatType, String chatTarget) {
  }

  public record LogReadRequest(
      String hokanContextToken,
      String scope,
      String protocol,
      String network,
      String chatType,
      String chatTarget,
      String date,
      Integer lines
  ) {
  }

  public record LogReadResponse(
      String scope,
      String protocol,
      String network,
      String chatType,
      String chatTarget,
      String date,
      boolean found,
      int lines,
      List<String> availableFiles,
      String content
  ) {
  }
}

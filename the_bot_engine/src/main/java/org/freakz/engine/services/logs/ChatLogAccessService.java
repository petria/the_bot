package org.freakz.engine.services.logs;

import org.freakz.common.chat.ChatIdentityUtil;
import org.freakz.common.users.BotPermission;
import org.freakz.common.util.TextUtils;
import org.freakz.engine.config.ConfigService;
import org.freakz.engine.services.ai.claw.HokanNodeContextTokenService;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Locale;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Service
@Primary
public class ChatLogAccessService {

  private static final Pattern LOG_DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
  private static final int DEFAULT_LINES = 80;
  private static final int MAX_LINES = 500;
  private static final int DEFAULT_MAX_DAYS = 30;
  private static final int MAX_SEARCH_DAYS = 365;
  private static final int DEFAULT_MAX_MATCHES = 20;
  private static final int MAX_SEARCH_MATCHES = 100;
  private static final int DEFAULT_MAX_BYTES = 16_000;
  private static final int MAX_SEARCH_BYTES = 64_000;

  private final ConfigService configService;
  private final HokanNodeContextTokenService tokenService;

  public ChatLogAccessService(ConfigService configService, HokanNodeContextTokenService tokenService) {
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

    String selectedDate = normalizeDate(request.date());
    List<String> availableFiles = includeAvailableFiles(request, selectedDate)
        ? listLogFiles(logDir)
        : List.of();
    if (selectedDate == null) {
      List<String> files = availableFiles.isEmpty() ? listLogFiles(logDir) : availableFiles;
      if (!files.isEmpty()) {
        selectedDate = files.getFirst().replaceFirst("\\.log$", "");
      }
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

  public LogSearchResponse searchLogs(LogSearchRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("missing request");
    }

    HokanNodeContextTokenService.VerifiedNodeContext context =
        tokenService.verifyToken(request.hokanContextToken());
    LogTarget target = resolveTarget(context, request);
    verifyPermission(context, target);

    SearchTerms terms = searchTerms(request);
    Path root = Path.of(configService.getBotLogDir()).toAbsolutePath().normalize();
    List<Path> logDirs = resolveSearchDirs(root, target, request);
    SearchBounds bounds = searchBounds(request);
    DateRange dateRange = dateRange(request, bounds.maxDays());
    List<LogSearchMatch> matches = new ArrayList<>();
    int searchedFiles = 0;
    int searchedLines = 0;
    int resultBytes = 0;
    boolean truncated = false;

    for (Path logDir : logDirs) {
      List<String> files = selectLogFiles(logDir, dateRange);
      for (String fileName : files) {
        Path logFile = logDir.resolve(fileName).normalize();
        if (!logFile.startsWith(logDir) || !Files.isRegularFile(logFile)) {
          continue;
        }

        searchedFiles++;
        String date = fileName.substring(0, fileName.length() - 4);
        try (BufferedReader reader = Files.newBufferedReader(logFile, StandardCharsets.UTF_8)) {
          String line;
          int lineNumber = 0;
          while ((line = reader.readLine()) != null) {
            lineNumber++;
            searchedLines++;
            ParsedLogLine parsed = parseLogLine(line);
            if (!matches(parsed, terms)) {
              continue;
            }

            LogSearchMatch match =
                new LogSearchMatch(date, lineNumber, parsed.time(), parsed.nick(), parsed.text());
            int matchBytes = estimateBytes(match);
            if (matches.size() >= bounds.maxMatches() || resultBytes + matchBytes > bounds.maxBytes()) {
              truncated = true;
              break;
            }
            matches.add(match);
            resultBytes += matchBytes;
          }
        } catch (IOException e) {
          throw new IllegalStateException("failed to search log file", e);
        }

        if (truncated) {
          break;
        }
      }
      if (truncated) {
        break;
      }
    }

    return new LogSearchResponse(
        target.scope(),
        target.protocol(),
        target.network(),
        target.chatType(),
        target.chatTarget(),
        searchedFiles,
        searchedLines,
        truncated,
        matches);
  }

  private LogTarget resolveTarget(
      HokanNodeContextTokenService.VerifiedNodeContext context,
      LogReadRequest request) {
    String scope = sanitizeScope(request.scope());
    String protocol = segment(TextUtils.firstNonBlank(request.protocol(), context.chatProtocol()), "chat");
    String network = segment(TextUtils.firstNonBlank(request.network(), context.network()), "unknown");
    String chatType = segment(TextUtils.firstNonBlank(request.chatType(), context.chatType()), "channel");
    String chatTarget = segment(TextUtils.firstNonBlank(request.chatTarget(), context.chatTarget()), "unknown");
    return new LogTarget(scope, protocol, network, chatType, chatTarget);
  }

  private LogTarget resolveTarget(
      HokanNodeContextTokenService.VerifiedNodeContext context,
      LogSearchRequest request) {
    String scope = sanitizeScope(request.scope());
    String protocol = segment(TextUtils.firstNonBlank(request.protocol(), context.chatProtocol()), "chat");
    String network = segment(TextUtils.firstNonBlank(request.network(), context.network()), "unknown");
    String chatType = segment(TextUtils.firstNonBlank(request.chatType(), context.chatType()), "channel");
    String chatTarget = "all-public-channels".equals(scope) && (request.chatTarget() == null || request.chatTarget().isBlank())
        ? "*"
        : segment(TextUtils.firstNonBlank(request.chatTarget(), context.chatTarget()), "unknown");
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
    return listLogFiles(logDir, 120);
  }

  private List<String> listLogFiles(Path logDir, int limit) {
    if (!Files.isDirectory(logDir)) {
      return List.of();
    }
    try (Stream<Path> stream = Files.list(logDir)) {
      Stream<String> names = stream
          .filter(Files::isRegularFile)
          .map(path -> path.getFileName().toString())
          .filter(name -> name.endsWith(".log"))
          .filter(name -> LOG_DATE.matcher(name.substring(0, name.length() - 4)).matches())
          .sorted(Comparator.reverseOrder());
      if (limit > 0) {
        names = names.limit(limit);
      }
      return names.toList();
    } catch (IOException e) {
      return List.of();
    }
  }

  private List<String> selectLogFiles(Path logDir, DateRange range) {
    return listLogFiles(logDir, 0).stream()
        .filter(name -> {
          LocalDate date = LocalDate.parse(name.substring(0, name.length() - 4));
          return !date.isBefore(range.from()) && !date.isAfter(range.to());
        })
        .toList();
  }

  private List<Path> resolveSearchDirs(Path root, LogTarget target, LogSearchRequest request) {
    Path base = root
        .resolve(target.protocol())
        .resolve(target.network())
        .resolve(target.chatType())
        .normalize();
    if (!base.startsWith(root)) {
      throw new IllegalArgumentException("invalid log target");
    }

    if ("all-public-channels".equals(target.scope())
        && "channel".equals(target.chatType())
        && (request.chatTarget() == null || request.chatTarget().isBlank())) {
      if (!Files.isDirectory(base)) {
        return List.of();
      }
      try (Stream<Path> stream = Files.list(base)) {
        return stream
            .filter(Files::isDirectory)
            .map(path -> path.toAbsolutePath().normalize())
            .filter(path -> path.startsWith(base))
            .sorted()
            .toList();
      } catch (IOException e) {
        return List.of();
      }
    }

    Path logDir = base.resolve(target.chatTarget()).normalize();
    if (!logDir.startsWith(base)) {
      throw new IllegalArgumentException("invalid log target");
    }
    return List.of(logDir);
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

  private DateRange dateRange(LogSearchRequest request, int maxDays) {
    String normalizedFrom = normalizeDate(request.dateFrom());
    String normalizedTo = normalizeDate(request.dateTo());
    LocalDate latest = normalizedTo == null ? LocalDate.now() : LocalDate.parse(normalizedTo);
    LocalDate earliest = normalizedFrom == null ? latest.minusDays(maxDays - 1L) : LocalDate.parse(normalizedFrom);
    if (earliest.isAfter(latest)) {
      throw new IllegalArgumentException("dateFrom must be on or before dateTo");
    }
    LocalDate boundedEarliest = latest.minusDays(maxDays - 1L);
    if (earliest.isBefore(boundedEarliest)) {
      earliest = boundedEarliest;
    }
    return new DateRange(earliest, latest);
  }

  private SearchBounds searchBounds(LogSearchRequest request) {
    return new SearchBounds(
        bounded(request.maxDays(), DEFAULT_MAX_DAYS, MAX_SEARCH_DAYS),
        bounded(request.maxMatches(), DEFAULT_MAX_MATCHES, MAX_SEARCH_MATCHES),
        bounded(request.maxBytes(), DEFAULT_MAX_BYTES, MAX_SEARCH_BYTES)
    );
  }

  private int bounded(Integer value, int defaultValue, int maxValue) {
    if (value == null || value < 1) {
      return defaultValue;
    }
    return Math.min(value, maxValue);
  }

  private SearchTerms searchTerms(LogSearchRequest request) {
    String query = normalizeTerm(request.query());
    List<String> anyTerms = normalizeTerms(request.anyTerms());
    List<String> allTerms = normalizeTerms(request.allTerms());
    String nick = normalizeTerm(request.nick());
    if (query == null && anyTerms.isEmpty() && allTerms.isEmpty() && nick == null) {
      throw new IllegalArgumentException("missing search terms");
    }
    return new SearchTerms(query, anyTerms, allTerms, nick);
  }

  private List<String> normalizeTerms(List<String> terms) {
    if (terms == null || terms.isEmpty()) {
      return List.of();
    }
    return terms.stream()
        .map(this::normalizeTerm)
        .filter(term -> term != null)
        .toList();
  }

  private String normalizeTerm(String term) {
    if (term == null || term.isBlank()) {
      return null;
    }
    return term.trim().toLowerCase(Locale.ROOT);
  }

  private boolean matches(ParsedLogLine line, SearchTerms terms) {
    if (terms.nick() != null) {
      String nick = line.nick() == null ? "" : line.nick().toLowerCase(Locale.ROOT);
      if (!nick.equals(terms.nick())) {
        return false;
      }
    }

    String haystack = (line.nick() + " " + line.text()).toLowerCase(Locale.ROOT);
    if (terms.query() != null && !haystack.contains(terms.query())) {
      return false;
    }
    for (String term : terms.allTerms()) {
      if (!haystack.contains(term)) {
        return false;
      }
    }
    if (!terms.anyTerms().isEmpty() && terms.anyTerms().stream().noneMatch(haystack::contains)) {
      return false;
    }
    return true;
  }

  private ParsedLogLine parseLogLine(String line) {
    String time = "";
    String rest = line == null ? "" : line;
    if (rest.length() >= 9 && rest.charAt(2) == ':' && rest.charAt(5) == ':' && rest.charAt(8) == ' ') {
      time = rest.substring(0, 8);
      rest = rest.substring(9);
    }

    int nickSeparator = rest.indexOf(": ");
    if (nickSeparator < 1) {
      return new ParsedLogLine(time, "", rest);
    }
    return new ParsedLogLine(time, rest.substring(0, nickSeparator), rest.substring(nickSeparator + 2));
  }

  private int estimateBytes(LogSearchMatch match) {
    return (match.date() + match.lineNumber() + match.time() + match.nick() + match.text())
        .getBytes(StandardCharsets.UTF_8).length + 64;
  }

  private boolean includeAvailableFiles(LogReadRequest request, String selectedDate) {
    return Boolean.TRUE.equals(request.includeAvailableFiles()) || selectedDate == null;
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

  private record LogTarget(String scope, String protocol, String network, String chatType, String chatTarget) {
  }

  private record DateRange(LocalDate from, LocalDate to) {
  }

  private record SearchBounds(int maxDays, int maxMatches, int maxBytes) {
  }

  private record SearchTerms(String query, List<String> anyTerms, List<String> allTerms, String nick) {
  }

  private record ParsedLogLine(String time, String nick, String text) {
  }

  public record LogReadRequest(
      String hokanContextToken,
      String scope,
      String protocol,
      String network,
      String chatType,
      String chatTarget,
      String date,
      Integer lines,
      Boolean includeAvailableFiles
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

  public record LogSearchRequest(
      String hokanContextToken,
      String scope,
      String protocol,
      String network,
      String chatType,
      String chatTarget,
      String nick,
      String query,
      List<String> anyTerms,
      List<String> allTerms,
      String dateFrom,
      String dateTo,
      Integer maxDays,
      Integer maxMatches,
      Integer maxBytes
  ) {
  }

  public record LogSearchResponse(
      String scope,
      String protocol,
      String network,
      String chatType,
      String chatTarget,
      int searchedFiles,
      int searchedLines,
      boolean truncated,
      List<LogSearchMatch> matches
  ) {
  }

  public record LogSearchMatch(
      String date,
      int lineNumber,
      String time,
      String nick,
      String text
  ) {
  }
}

package org.freakz.engine.services.ai.hermes;

import org.freakz.common.chat.ChatIdentityUtil;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.users.BotPermission;
import org.freakz.common.users.UserPermissions;
import org.freakz.common.util.TextUtils;
import org.freakz.engine.config.ConfigService;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
public class HermesPromptContextService {

  private static final ZoneId CHAT_ZONE = ZoneId.of("Europe/Helsinki");

  private final ConfigService configService;
  private final HermesSettingsService settingsService;

  public HermesPromptContextService(ConfigService configService, HermesSettingsService settingsService) {
    this.configService = configService;
    this.settingsService = settingsService;
  }

  public String buildChatInput(EngineRequest request, String sessionKey, String userPrompt) {
    return buildContext(request, sessionKey, List.of("logs.read", "logs.search"), null)
        + "\n[USER_PROMPT]\n"
        + (userPrompt == null ? "" : userPrompt)
        + "\n[/USER_PROMPT]";
  }

  public String buildAiCommandInput(
      EngineRequest request,
      String sessionKey,
      String commandName,
      String argumentsText,
      List<String> allowedTools) {
    return buildContext(request, sessionKey, allowedTools, commandName)
        + "\n[COMMAND_INVOCATION]\n"
        + "command=!" + TextUtils.nullToEmpty(commandName) + "\n"
        + "arguments=" + safe(argumentsText) + "\n"
        + "[/COMMAND_INVOCATION]";
  }

  private String buildContext(
      EngineRequest request,
      String sessionKey,
      List<String> availableTools,
      String commandName) {
    ChatContext chat = chatContext(request);
    List<String> effectivePermissions = UserPermissions.effective(request == null ? null : request.getUser());
    boolean hasAllPermissions = UserPermissions.has(request == null ? null : request.getUser(), BotPermission.ALL);
    List<String> availableLogFiles = listAvailableLogFiles(chat);

    StringBuilder sb = new StringBuilder();
    sb.append("[HOKAN_CONTEXT v2]\n");
    sb.append("ai_backend=hermes\n");
    sb.append("bot_instance_id=").append(settingsService.getBotInstanceId()).append("\n");
    sb.append("session_key=").append(sessionKey).append("\n");
    sb.append("timestamp=").append(OffsetDateTime.now(CHAT_ZONE)).append("\n");
    sb.append("timestamp_timezone=Europe/Helsinki\n");
    sb.append("echo_to_alias=").append(safe(request == null ? null : request.getEchoToAlias())).append("\n");
    sb.append("source=").append(chat.protocol()).append("\n");
    sb.append("network=").append(chat.network()).append("\n");
    sb.append("chat_type=").append(chat.chatType()).append("\n");
    sb.append("channel=").append(chat.chatTarget()).append("\n");
    sb.append("chat_id=").append(chat.chatId()).append("\n");
    sb.append("sender_nick=").append(safe(request == null ? null : request.getFromSender())).append("\n");
    sb.append("sender_id=").append(safe(request == null ? null : request.getFromSenderId())).append("\n");
    sb.append("sender_name=").append(senderName(request)).append("\n");
    sb.append("permissions=").append(String.join(",", effectivePermissions)).append("\n");
    sb.append("permissions_all=").append(hasAllPermissions).append("\n");
    appendUserIdentity(sb, request);
    if (commandName != null && !commandName.isBlank()) {
      sb.append("runtime_ai_command=").append(safe(commandName)).append("\n");
    }

    sb.append("\n");
    appendOutputPolicy(sb, chat);
    appendLogPolicy(sb, chat, availableTools, availableLogFiles);

    if ("channel".equals(chat.chatType())) {
      sb.append("public_channel_participation=true\n");
      sb.append("public_channel_reply_rule=reply only when you add clear value to the ongoing public conversation\n");
      sb.append("public_channel_silence_allowed=true\n");
      sb.append("public_channel_avoid_interrupting=true\n");
      sb.append("public_channel_prefer_brief_replies=true\n");
    }

    sb.append("[/HOKAN_CONTEXT]\n");
    return sb.toString();
  }

  private void appendOutputPolicy(StringBuilder sb, ChatContext chat) {
    sb.append("assistant_identity=the_bot\n");
    sb.append("assistant_display_name=Hokan\n");
    sb.append("assistant_backend_hidden=true\n");
    sb.append("assistant_must_not_mention_backend=true\n");
    sb.append("assistant_style=reply as the_bot only\n");
    if ("irc".equals(chat.protocol())) {
      sb.append("output_policy=compact\n");
      sb.append("output_max_chars=380\n");
      sb.append("output_max_lines=").append("channel".equals(chat.chatType()) ? 2 : 4).append("\n");
      sb.append("output_prefer_single_message=true\n");
      sb.append("output_avoid_markdown_tables=true\n");
      sb.append("output_no_code_blocks=true\n");
    }
    sb.append("final_reply_must_contain_result_or_explicit_failure=true\n");
    sb.append("final_reply_must_not_be_placeholder_progress=true\n");
    sb.append("final_reply_must_not_only_promise_future_action=true\n");
    sb.append("final_reply_forbid_phrases=checking now|looking it up now|i will check|let me check|hold on while i check\n");
  }

  private void appendLogPolicy(
      StringBuilder sb,
      ChatContext chat,
      List<String> availableTools,
      List<String> availableLogFiles) {
    boolean logsRead = availableTools != null && availableTools.contains("logs.read");
    boolean logsSearch = availableTools != null && availableTools.contains("logs.search");
    sb.append("log_access_mode=controlled_hermes_tool\n");
    sb.append("log_file_name_format=yyyy-mm-dd.log\n");
    sb.append("log_file_name_date_meaning=each log filename date is the chat day in Europe/Helsinki\n");
    if (!availableLogFiles.isEmpty()) {
      sb.append("log_dir_files=").append(String.join(",", availableLogFiles)).append("\n");
      sb.append("log_dir_file_count=").append(availableLogFiles.size()).append("\n");
    }
    sb.append("log_hint_lines=80\n");
    sb.append("local_file_access_allowed=false\n");
    sb.append("log_file_may_be_read_directly=false\n");
    sb.append("log_directory_may_be_inspected_when_supported=false\n");
    sb.append("tool_usage_rule=if you decide to check, fetch, inspect, open, search, read, or verify something, request an allowed tool first and only then send the final user-visible reply\n");
    sb.append("tool_failure_rule=if the work cannot be completed, say that clearly in the final reply with the reason\n");
    sb.append("log_access_rule=do not read local log files directly and do not use HTTP for logs; request logs only through allowed Hermes tools\n");
    sb.append("log_api_scope_rule=use current-chat by default; use broader scopes only when the user asks and permissions permit it\n");
    sb.append("directory_scan_rule=request logs.read without date and with includeAvailableFiles=true to discover available files for the permitted target\n");
    sb.append("hermes_log_tools_available=").append(logsRead || logsSearch).append("\n");
    if (logsSearch) {
      sb.append("hermes_log_search_tool=logs.search\n");
      sb.append("hermes_log_search_tool_params={\"scope\":\"current-chat\",\"query\":\"<required phrase>\",\"allTerms\":[\"<term>\"],\"anyTerms\":[\"<term>\"],\"nick\":\"<nick optional>\",\"dateFrom\":\"<yyyy-mm-dd optional>\",\"dateTo\":\"<yyyy-mm-dd optional>\",\"maxDays\":30,\"maxMatches\":20,\"maxBytes\":16000}\n");
      sb.append("hermes_log_search_tool_use=for historical questions with concrete words, names, phrases, or nicks, request logs.search first; use compact terms from the user question\n");
      sb.append("hermes_log_search_tool_returns=json with result.searchedFiles, result.searchedLines, result.truncated, result.matches[{date,lineNumber,time,nick,text}]\n");
    }
    if (logsRead) {
      sb.append("hermes_log_read_tool=logs.read\n");
      sb.append("hermes_log_read_tool_params={\"scope\":\"current-chat\",\"date\":\"<yyyy-mm-dd optional>\",\"lines\":80,\"includeAvailableFiles\":false}\n");
      sb.append("hermes_log_read_tool_use=for recent chat context, broad channel analysis, ranking questions, or exact dated tail reads, request logs.read; omit date to read latest available log\n");
      sb.append("hermes_log_read_tool_returns=json with result.content, result.found, result.date, result.availableFiles when requested or date omitted\n");
    }
    sb.append("recent_messages_source=controlled_hermes_log_tool\n");
    sb.append("recent_messages=not inlined by bot-engine; request logs.read for recent context when needed\n");
  }

  private ChatContext chatContext(EngineRequest request) {
    String protocol = ChatIdentityUtil.sanitize(
        request == null ? null : request.getChatProtocol(),
        ChatIdentityUtil.resolveProtocol(request == null ? null : request.getNetwork()));
    String network = ChatIdentityUtil.sanitize(request == null ? null : request.getNetwork(), "unknown");
    String chatType = ChatIdentityUtil.sanitize(
        request == null ? null : request.getChatType(),
        request != null && request.isPrivateChannel() ? "dm" : "channel");
    String chatId = request == null ? null : request.getChatId();
    String chatTarget;
    if (chatId != null && !chatId.isBlank()) {
      chatTarget = ChatIdentityUtil.extractTargetFromChatId(
          chatId,
          ChatIdentityUtil.sanitize(request.getReplyTo(), "unknown"));
    } else {
      chatTarget = ChatIdentityUtil.sanitize(request == null ? null : request.getReplyTo(), "unknown");
      chatId = ChatIdentityUtil.buildChatId(protocol, network, chatType, chatTarget);
    }
    return new ChatContext(protocol, network, chatType, chatTarget, chatId);
  }

  private List<String> listAvailableLogFiles(ChatContext chat) {
    try {
      Path logDir = Path.of(configService.getBotLogDir())
          .resolve(chat.protocol())
          .resolve(chat.network())
          .resolve(chat.chatType())
          .resolve(chat.chatTarget())
          .normalize();
      if (!Files.isDirectory(logDir)) {
        return List.of();
      }
      try (Stream<Path> stream = Files.list(logDir)) {
        return stream
            .filter(Files::isRegularFile)
            .map(path -> path.getFileName().toString())
            .filter(name -> name.endsWith(".log"))
            .sorted(Comparator.reverseOrder())
            .limit(60)
            .toList();
      }
    } catch (Exception e) {
      return List.of();
    }
  }

  private void appendUserIdentity(StringBuilder sb, EngineRequest request) {
    if (request == null || request.getUser() == null) {
      return;
    }
    sb.append("requested_by_username=").append(safe(request.getUser().getUsername())).append("\n");
    sb.append("requested_by_name=").append(safe(request.getUser().getName())).append("\n");
    sb.append("requested_by_irc_nick=").append(safe(request.getUser().getIrcNick())).append("\n");
    sb.append("requested_by_telegram_id=").append(safe(request.getUser().getTelegramId())).append("\n");
    sb.append("requested_by_discord_id=").append(safe(request.getUser().getDiscordId())).append("\n");
    sb.append("requested_by_whatsapp_id=").append(safe(request.getUser().getWhatsappId())).append("\n");
  }

  private String senderName(EngineRequest request) {
    if (request == null || request.getUser() == null || request.getUser().getName() == null) {
      return safe(request == null ? null : request.getFromSender());
    }
    return safe(request.getUser().getName());
  }

  private String safe(String value) {
    return value == null ? "" : value.replaceAll("[\\r\\n]+", " ").trim();
  }

  private record ChatContext(String protocol, String network, String chatType, String chatTarget, String chatId) {
  }
}

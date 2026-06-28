package org.freakz.engine.commands.handlers.admin;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.UnflaggedOption;
import org.freakz.common.model.botconfig.Channel;
import org.freakz.common.model.botconfig.IrcServerConfig;
import org.freakz.common.model.botconfig.TheBotConfig;
import org.freakz.common.model.connectionmanager.BotConnectionChannelResponse;
import org.freakz.common.model.connectionmanager.BotConnectionResponse;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.model.users.User;
import org.freakz.common.users.BotPermission;
import org.freakz.common.users.ChannelPermissionUtil;
import org.freakz.common.users.UserPermissions;
import org.freakz.engine.commands.annotations.HokanAdminCommand;
import org.freakz.engine.commands.annotations.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;
import org.freakz.engine.dto.ConnectionsResponse;
import org.freakz.engine.services.api.ServiceRequestType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;

@HokanCommandHandler
@HokanAdminCommand
public class UserPrivCmd extends AbstractCmd {

  private static final String ARG_ARGS = "args";
  private static final String LEGACY_LIVE_VIEW_ALL = "live-channels.view.all";
  private static final String LEGACY_LIVE_SEND_ALL = "live-channels.send.all";
  private static final String LEGACY_LIVE_VIEW_PREFIX = "live-channels.view.";
  private static final String LEGACY_LIVE_SEND_PREFIX = "live-channels.send.";

  @Override
  public void initCommandOptions(JSAP jsap) throws JSAPException {
    jsap.setHelp("Manage users.json channel privileges.");
    UnflaggedOption opt = new UnflaggedOption(ARG_ARGS).setRequired(false).setGreedy(true);
    jsap.registerParameter(opt);
  }

  @Override
  public String executeCommand(EngineRequest request, JSAPResult results) {
    List<String> args = Arrays.stream(results.getStringArray(ARG_ARGS))
        .filter(value -> value != null && !value.isBlank())
        .toList();
    if (args.isEmpty()) {
      return usage();
    }

    return switch (normalize(args.get(0))) {
      case "list" -> listUsers();
      case "show" -> showUser(args);
      case "channels" -> listChannels(request);
      case "grant" -> updateUserPermissions(request, args, true);
      case "revoke" -> updateUserPermissions(request, args, false);
      case "migrate" -> migrateUsers(request, args);
      default -> "Unknown userpriv action: " + args.get(0) + "\n" + usage();
    };
  }

  private String listUsers() {
    List<User> users = getBotEngine().getUsersService().findAll().stream()
        .filter(User.class::isInstance)
        .map(User.class::cast)
        .sorted((left, right) -> nullSafe(left.getUsername()).compareToIgnoreCase(nullSafe(right.getUsername())))
        .toList();
    if (users.isEmpty()) {
      return "No users found.";
    }
    List<String> lines = new ArrayList<>();
    lines.add("== User privileges");
    for (User user : users) {
      lines.add(user.getUsername() + ": " + summarizePermissions(user.getPermissions()));
    }
    return String.join("\n", lines);
  }

  private String showUser(List<String> args) {
    if (args.size() != 2) {
      return "Usage: !userpriv show <username>";
    }
    User user = findUser(args.get(1)).orElse(null);
    if (user == null) {
      return "No user found: " + args.get(1);
    }
    List<String> permissions = UserPermissions.normalize(user.getPermissions());
    return "== Privileges for " + user.getUsername() + "\n"
        + (permissions.isEmpty() ? "(none)" : String.join("\n", permissions));
  }

  private String listChannels(EngineRequest request) {
    List<ChannelTarget> channels = knownChannels(request);
    if (channels.isEmpty()) {
      return "No known public channels found.";
    }
    List<String> lines = new ArrayList<>();
    lines.add("== Known channels");
    for (ChannelTarget channel : channels) {
      lines.add(channel.echoToAlias() + " [" + ChannelPermissionUtil.connectionKey(channel.connectionType()) + "]");
    }
    return String.join("\n", lines);
  }

  private String updateUserPermissions(EngineRequest request, List<String> args, boolean grant) {
    if (args.size() != 4) {
      return "Usage: !userpriv " + (grant ? "grant" : "revoke")
          + " <username> view|send all|irc|discord|telegram|whatsapp|<echoToAlias>";
    }

    String username = args.get(1);
    String mode = normalize(args.get(2));
    if (!"view".equals(mode) && !"send".equals(mode)) {
      return "Privilege mode must be view or send.";
    }

    PermissionTarget target = resolveTarget(request, args.get(3));
    if (target == null) {
      return "Could not resolve privilege target: " + args.get(3)
          + "\nUse !userpriv channels to list channel aliases.";
    }

    List<String> permissions = permissionsFor(mode, target, grant);
    User updated;
    try {
      updated = getBotEngine().getUsersService().updateByUsername(username, user -> {
        TreeSet<String> values = new TreeSet<>(UserPermissions.normalize(user.getPermissions()));
        if (grant) {
          values.addAll(permissions);
        } else {
          values.removeAll(permissions);
        }
        user.setPermissions(values.stream().toList());
        return user;
      });
      getBotEngine().getUsersService().reloadUsers();
    } catch (IllegalArgumentException e) {
      return e.getMessage();
    }

    String action = grant ? "Granted" : "Revoked";
    return action + " " + String.join(", ", permissions)
        + " for " + updated.getUsername()
        + "\nNow: " + summarizePermissions(updated.getPermissions());
  }

  private String migrateUsers(EngineRequest request, List<String> args) {
    if (args.size() != 2) {
      return "Usage: !userpriv migrate <username|all>";
    }
    List<User> users = getBotEngine().getUsersService().findAll().stream()
        .filter(User.class::isInstance)
        .map(User.class::cast)
        .filter(user -> "all".equalsIgnoreCase(args.get(1))
            || normalize(user.getUsername()).equals(normalize(args.get(1))))
        .toList();
    if (users.isEmpty()) {
      return "No user found: " + args.get(1);
    }

    List<ChannelTarget> channels = knownChannels(request);
    List<String> lines = new ArrayList<>();
    for (User user : users) {
      MigrationResult result;
      try {
        result = migrateUser(user.getUsername(), channels);
      } catch (IllegalArgumentException e) {
        lines.add(user.getUsername() + ": " + e.getMessage());
        continue;
      }
      lines.add(user.getUsername() + ": added " + result.added()
          + ", removed legacy " + result.removed()
          + (result.unresolved().isEmpty() ? "" : ", unresolved " + String.join(",", result.unresolved())));
    }
    getBotEngine().getUsersService().reloadUsers();
    return String.join("\n", lines);
  }

  private MigrationResult migrateUser(String username, List<ChannelTarget> channels) {
    final MigrationResult[] result = new MigrationResult[1];
    getBotEngine().getUsersService().updateByUsername(username, user -> {
      TreeSet<String> permissions = new TreeSet<>(UserPermissions.normalize(user.getPermissions()));
      TreeSet<String> updated = new TreeSet<>(permissions);
      List<String> unresolved = new ArrayList<>();
      int added = 0;
      int removed = 0;

      if (updated.remove(LEGACY_LIVE_VIEW_ALL)) {
        removed++;
        if (updated.add(BotPermission.CHANNELS_VIEW_ALL)) {
          added++;
        }
      }
      if (updated.remove(LEGACY_LIVE_SEND_ALL)) {
        removed++;
        if (updated.add(BotPermission.CHANNELS_VIEW_ALL)) {
          added++;
        }
        if (updated.add(BotPermission.CHANNELS_SEND_ALL)) {
          added++;
        }
      }

      for (String permission : permissions) {
        if (permission.startsWith(LEGACY_LIVE_VIEW_PREFIX)) {
          ChannelTarget channel = findChannelByLegacyKey(channels, permission.substring(LEGACY_LIVE_VIEW_PREFIX.length()));
          if (channel == null) {
            unresolved.add(permission);
            continue;
          }
          if (updated.remove(permission)) {
            removed++;
          }
          if (updated.add(ChannelPermissionUtil.viewPermission(channel.connectionType(), channel.echoToAlias()))) {
            added++;
          }
        } else if (permission.startsWith(LEGACY_LIVE_SEND_PREFIX)) {
          ChannelTarget channel = findChannelByLegacyKey(channels, permission.substring(LEGACY_LIVE_SEND_PREFIX.length()));
          if (channel == null) {
            unresolved.add(permission);
            continue;
          }
          if (updated.remove(permission)) {
            removed++;
          }
          if (updated.add(ChannelPermissionUtil.viewPermission(channel.connectionType(), channel.echoToAlias()))) {
            added++;
          }
          if (updated.add(ChannelPermissionUtil.sendPermission(channel.connectionType(), channel.echoToAlias()))) {
            added++;
          }
        }
      }

      user.setPermissions(updated.stream().toList());
      result[0] = new MigrationResult(added, removed, unresolved);
      return user;
    });
    return result[0];
  }

  private List<String> permissionsFor(String mode, PermissionTarget target, boolean grant) {
    List<String> permissions = new ArrayList<>();
    boolean includeView = "view".equals(mode) || grant;
    boolean includeSend = "send".equals(mode) || (!grant && "view".equals(mode));
    if ("all".equals(target.kind())) {
      if (includeView) {
        permissions.add(BotPermission.CHANNELS_VIEW_ALL);
      }
      if (includeSend) {
        permissions.add(BotPermission.CHANNELS_SEND_ALL);
      }
      return permissions;
    }

    if ("type".equals(target.kind())) {
      if (includeView) {
        permissions.add(ChannelPermissionUtil.viewTypePermission(target.connectionType()));
      }
      if (includeSend) {
        permissions.add(ChannelPermissionUtil.sendTypePermission(target.connectionType()));
      }
      return permissions;
    }

    if (includeView) {
      permissions.add(ChannelPermissionUtil.viewPermission(target.connectionType(), target.echoToAlias()));
    }
    if (includeSend) {
      permissions.add(ChannelPermissionUtil.sendPermission(target.connectionType(), target.echoToAlias()));
    }
    return permissions;
  }

  private PermissionTarget resolveTarget(EngineRequest request, String rawTarget) {
    String target = normalize(rawTarget);
    if ("all".equals(target)) {
      return new PermissionTarget("all", null, null);
    }
    if (ChannelPermissionUtil.isSupportedConnectionKey(target)) {
      return new PermissionTarget("type", target, null);
    }
    return knownChannels(request).stream()
        .filter(channel -> channel.echoToAlias().equalsIgnoreCase(rawTarget)
            || ChannelPermissionUtil.channelKey(channel.echoToAlias()).equals(target))
        .findFirst()
        .map(channel -> new PermissionTarget("channel", channel.connectionType(), channel.echoToAlias()))
        .orElse(null);
  }

  private List<ChannelTarget> knownChannels(EngineRequest request) {
    Map<String, ChannelTarget> channels = new LinkedHashMap<>();
    try {
      ConnectionsResponse response = doServiceRequest(request, null, ServiceRequestType.ConnectionControlService);
      if (response != null && response.getConnectionMap() != null) {
        for (BotConnectionResponse connection : response.getConnectionMap().values()) {
          if (connection == null || connection.getChannels() == null) {
            continue;
          }
          for (BotConnectionChannelResponse channel : connection.getChannels()) {
            addChannel(channels, channel.getEchoToAlias(),
                channel.getType() == null || channel.getType().isBlank() ? connection.getType() : channel.getType());
          }
        }
      }
    } catch (RuntimeException ignored) {
      // Configured channels below are enough for permission editing when bot-io is temporarily unavailable.
    }

    TheBotConfig botConfig = request == null ? null : request.getBotConfig();
    if (botConfig != null) {
      for (IrcServerConfig ircConfig : nullSafe(botConfig.getIrcServerConfigs())) {
        for (Channel channel : nullSafe(ircConfig == null ? null : ircConfig.getChannelList())) {
          addChannel(channels, channel.getEchoToAlias(), "IRC_CONNECTION");
        }
      }
      for (Channel channel : nullSafe(botConfig.getDiscordConfig() == null ? null : botConfig.getDiscordConfig().getChannelList())) {
        addChannel(channels, channel.getEchoToAlias(), "DISCORD_CONNECTION");
      }
      for (Channel channel : nullSafe(botConfig.getTelegramConfig() == null ? null : botConfig.getTelegramConfig().getChannelList())) {
        addChannel(channels, channel.getEchoToAlias(), "TELEGRAM_CONNECTION");
      }
      for (Channel channel : nullSafe(botConfig.getWhatsappConfig() == null ? null : botConfig.getWhatsappConfig().getChannelList())) {
        addChannel(channels, channel.getEchoToAlias(), "WHATSAPP_CONNECTION");
      }
    }

    return channels.values().stream()
        .sorted((left, right) -> left.echoToAlias().compareToIgnoreCase(right.echoToAlias()))
        .toList();
  }

  private void addChannel(Map<String, ChannelTarget> channels, String echoToAlias, String connectionType) {
    if (echoToAlias == null || echoToAlias.isBlank() || connectionType == null || connectionType.isBlank()) {
      return;
    }
    channels.putIfAbsent(
        ChannelPermissionUtil.channelKey(echoToAlias),
        new ChannelTarget(connectionType, echoToAlias.trim()));
  }

  private ChannelTarget findChannelByLegacyKey(List<ChannelTarget> channels, String legacyKey) {
    if (legacyKey == null || legacyKey.isBlank()) {
      return null;
    }
    String normalized = ChannelPermissionUtil.channelKey(legacyKey);
    return channels.stream()
        .filter(channel -> ChannelPermissionUtil.channelKey(channel.echoToAlias()).equals(normalized))
        .findFirst()
        .orElse(null);
  }

  private Optional<User> findUser(String username) {
    String normalized = normalize(username);
    return getBotEngine().getUsersService().findAll().stream()
        .filter(User.class::isInstance)
        .map(User.class::cast)
        .filter(user -> normalize(user.getUsername()).equals(normalized))
        .findFirst();
  }

  private String summarizePermissions(List<String> permissions) {
    List<String> normalized = UserPermissions.normalize(permissions);
    if (normalized.isEmpty()) {
      return "(none)";
    }
    long channelCount = normalized.stream().filter(permission -> permission.startsWith("channels.")).count();
    long otherCount = normalized.size() - channelCount;
    return channelCount + " channel, " + otherCount + " other";
  }

  private String usage() {
    return "Usage: !userpriv list | show <user> | channels | grant <user> view|send all|irc|discord|telegram|whatsapp|<alias> | revoke <user> view|send ... | migrate <user|all>";
  }

  private String normalize(String value) {
    return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
  }

  private String nullSafe(String value) {
    return value == null ? "" : value;
  }

  private <T> List<T> nullSafe(List<T> values) {
    return values == null ? List.of() : values;
  }

  private record PermissionTarget(String kind, String connectionType, String echoToAlias) {
  }

  private record ChannelTarget(String connectionType, String echoToAlias) {
  }

  private record MigrationResult(int added, int removed, List<String> unresolved) {
  }
}

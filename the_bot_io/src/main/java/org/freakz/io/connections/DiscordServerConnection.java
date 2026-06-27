package org.freakz.io.connections;

import org.freakz.common.exception.BotIOException;
import org.freakz.common.model.botconfig.DiscordConfig;
import org.freakz.common.model.connectionmanager.ChannelUser;
import org.freakz.common.model.feed.Message;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.Channel;
import org.javacord.api.entity.channel.PrivateChannel;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.MessageAttachment;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class DiscordServerConnection extends BotConnection {

  private static final Logger log = LoggerFactory.getLogger(DiscordServerConnection.class);

  private final EventPublisher publisher;
  private DiscordApi api;
  private ConnectionManager connectionManager;
  private DiscordConfig config;
  private String botName;

  public DiscordServerConnection(EventPublisher publisher) {
    super(BotConnectionType.DISCORD_CONNECTION);
    this.publisher = publisher;
  }

  @Override
  public String getNetwork() {
    return "Discord";
  }

  public void init(ConnectionManager connectionManager, String botName, DiscordConfig config) {

    this.connectionManager = connectionManager;
    this.config = config;
    this.botName = botName;

    String token = config.getToken();
    this.api = new DiscordApiBuilder()
        .addMessageCreateListener(this::messageListener)
        .addServerBecomesAvailableListener(event -> {
          log.debug("loaded: {}", event);
          try {
            updateChannelMap(event.getApi());
          } catch (BotIOException e) {
            throw new RuntimeException(e);
          }
        })
        .setAllIntents()
        .setUserCacheEnabled(true)
        .setToken(token)
        .setWaitForServersOnStartup(false)
        .login()
        .join();

  }

  @Override
  public void stop() {
    if (api != null) {
      api.disconnect();
    }
  }

  private void updateChannelMap(DiscordApi api) throws BotIOException {
    Set<Server> servers = api.getServers();
    for (Server server : servers) {
      for (Channel channel : server.getChannels()) {

        org.freakz.common.model.botconfig.Channel ch = resolveByEchoTo(channel.getId());
        if (ch == null) {
//                    log.error("No Channel config found with: " + channel);
          continue;
        }

        JoinedChannelContainer container = this.connectionManager.getJoinedChannelContainer(ch.getEchoToAlias());
        BotConnectionChannel botConnectionChannel;
        if (container == null) {
          botConnectionChannel = new BotConnectionChannel();
          botConnectionChannel.setName(ch.getName());
          botConnectionChannel.setId(String.valueOf(channel.getId()));
          botConnectionChannel.setType(getType().name());
          botConnectionChannel.setNetwork(getNetwork());
          botConnectionChannel.setEchoToAlias(ch.getEchoToAlias());

        } else {
          botConnectionChannel = container.channel;
        }
        botConnectionChannel.setConfigured(true);
        this.connectionManager.updateJoinedChannelsMap(BotConnectionType.DISCORD_CONNECTION, this, botConnectionChannel);
        log.debug("Updated channel: {}", botConnectionChannel);
      }
    }

  }

  private org.freakz.common.model.botconfig.Channel resolveByEchoTo(long id) {
    for (org.freakz.common.model.botconfig.Channel channel : this.config.getChannelList()) {
      if (channel.getId().equals("" + id)) {
        return channel;
      }
    }
    return null;
  }

  @Override
  public List<ChannelUser> getChannelUsersByEchoToAlias(String echoToAlias, BotConnectionChannel channel) {
    if (api == null || channel == null || channel.getId() == null || channel.getId().isBlank()) {
      return List.of();
    }
    Optional<ServerTextChannel> textChannel = api.getServers().stream()
        .map(server -> server.getTextChannelById(channel.getId()))
        .flatMap(Optional::stream)
        .findFirst();
    if (textChannel.isEmpty()) {
      return List.of();
    }

    ServerTextChannel discordChannel = textChannel.get();
    Server server = discordChannel.getServer();
    if (!server.hasAllMembersInCache()) {
      server.requestMembersChunks();
    }

    return server.getMembers().stream()
        .filter(user -> server.getVisibleChannels(user).contains(discordChannel))
        .sorted(Comparator.comparing(user -> user.getDisplayName(server), String.CASE_INSENSITIVE_ORDER))
        .map(user -> toChannelUser(echoToAlias, channel, server, user))
        .toList();
  }

  private ChannelUser toChannelUser(String echoToAlias, BotConnectionChannel channel, Server server, User user) {
    String userId = user.getIdAsString();
    String displayName = user.getDisplayName(server);
    String username = user.getName();
    this.connectionManager.markUserSeen(
        this,
        echoToAlias,
        userId,
        username,
        displayName,
        "DISCORD_MEMBERS",
        channel.getId(),
        channel.getName());
    return ChannelUser.builder()
        .account(userId)
        .nick(displayName)
        .realName(username)
        .server(server.getName())
        .userString(user.getMentionTag())
        .operatorInformation(user.isBot() ? "bot" : null)
        .build();
  }

  @Override
  public void sendMessageTo(Message message) {
    Optional<Channel> resolvedChannel = resolveChannel(message);
    if (resolvedChannel.isPresent()) {
      Channel channel = resolvedChannel.get();
      String outgoingMessage = formatOutgoingMessage(message.getMessage());
      Optional<ServerTextChannel> serverTextChannel = channel.asServerTextChannel();
      if (serverTextChannel.isPresent()) {
        serverTextChannel.get().sendMessage(outgoingMessage).join();
        return;
      }
      Optional<PrivateChannel> privateChannel = channel.asPrivateChannel();
      if (privateChannel.isPresent()) {
        privateChannel.get().sendMessage(outgoingMessage).join();
        return;
      }
      throw new RuntimeException("Could not send Discord message to unsupported channel: " + message.getTarget());
    } else {
      throw new RuntimeException("Can't send Discord message to: " + message.getTarget());
    }

  }

  @Override
  public void sendProcessingIndicator(Message message) {
    resolveChannel(message)
        .flatMap(Channel::asTextChannel)
        .ifPresent(channel -> channel.type().exceptionally(e -> {
          log.debug("Discord typing indicator failed: {}", e.getMessage());
          return null;
        }));
  }

  private Optional<Channel> resolveChannel(Message message) {
    Set<Channel> channels = api.getChannels();
    for (Channel ch : channels) {
      String chId = "" + ch.getId();
      if (chId.equals(message.getId())) {
        return Optional.of(ch);
      }
      String t = ch.toString();
      if (t.contains("name: " + message.getTarget())) {
        return Optional.of(ch);
      }
    }
    Optional<String> privateUserId = resolvePrivateUserId(message);
    if (privateUserId.isPresent()) {
      try {
        User user = api.getUserById(privateUserId.get()).join();
        PrivateChannel privateChannel = user.openPrivateChannel().join();
        return Optional.of(privateChannel);
      } catch (RuntimeException e) {
        log.warn("Could not resolve Discord private channel for user {}", privateUserId.get(), e);
      }
    }
    return Optional.empty();
  }

  private Optional<String> resolvePrivateUserId(Message message) {
    String target = clean(message.getTarget());
    if (target != null && target.startsWith("PRIVATE-DISCORD-")) {
      return Optional.of(target.substring("PRIVATE-DISCORD-".length()));
    }
    String id = clean(message.getId());
    if (id != null && id.matches("\\d+")) {
      return Optional.of(id);
    }
    return Optional.empty();
  }

  static String formatOutgoingMessage(String message) {
    if (message == null || message.isBlank()) {
      return message;
    }
    if (message.lines().count() > 1) {
      return String.format("```%s```", message);
    }
    return message;
  }

  private void messageListener(MessageCreateEvent event) {

    log.debug("Discord msg: {}", event.toString());
    MessageAuthor messageAuthor = event.getMessageAuthor();
    if (isOwnDiscordMessage(messageAuthor)) {
      log.debug("Ignore own Discord message");
      return;
    }

    boolean isPrivate = !event.getServer().isPresent();
    String echoToAlias = null;
    org.freakz.common.model.botconfig.Channel configuredChannel = resolveConfiguredChannel(event);
    if (configuredChannel != null) {
      echoToAlias = configuredChannel.getEchoToAlias();
    } else if (isPrivate && messageAuthor.asUser().isPresent()) {
      echoToAlias = "PRIVATE-DISCORD-" + messageAuthor.asUser().get().getIdAsString();
    }
    this.connectionManager.markMessageReceived(
        echoToAlias,
        event.getMessageAuthor().getName(),
        "Discord",
        getType().toString(),
        getNetwork(),
        isPrivate ? "Discord DM " + event.getMessageAuthor().getName() : null
    );
    markDiscordUserSeen(echoToAlias, event, isPrivate);
    publisher.publishEvent(this, event, echoToAlias);

    try {
      updateChannelMap(event.getApi());
    } catch (BotIOException e) {
      throw new RuntimeException(e);
    }

    String channelStr = event.getChannel().toString();
    // "ServerTextChannel (id: 1033431599708123278, name: hokandev)"
    int idx1 = channelStr.indexOf("name: ");
    String channelName = channelStr.substring(idx1 + 6, channelStr.length() - 1).replaceAll("\\)|]", "");
    log.debug("replyTo: '{}'", channelName);

    if (messageAuthor.asUser().isPresent()) {
      StringBuilder messageTxt = new StringBuilder(event.getMessage().getContent());
      if (!event.getMessage().getAttachments().isEmpty()) {
        for (MessageAttachment attachment : event.getMessageAttachments()) {
          messageTxt.append(" [");
          messageTxt.append(attachment.getUrl().toString());
          messageTxt.append("]");
        }
      }
      BridgeEchoService.echoToConfiguredTargets(
          this.connectionManager,
          configuredChannel,
          "Discord",
          event.getMessageAuthor().getName(),
          messageTxt.toString(),
          botName);
    }
  }

  private boolean isOwnDiscordMessage(MessageAuthor messageAuthor) {
    if (messageAuthor.isYourself()) {
      return true;
    }
    String configuredBotUserId = clean(this.config.getTheBotUserId());
    return configuredBotUserId != null
        && messageAuthor.asUser()
            .map(user -> configuredBotUserId.equals(user.getIdAsString()))
            .orElse(false);
  }

  private void markDiscordUserSeen(String echoToAlias, MessageCreateEvent event, boolean isPrivate) {
    String userId = event.getMessageAuthor().asUser()
        .map(user -> user.getIdAsString())
        .orElse(String.valueOf(event.getMessageAuthor().getId()));
    String displayName = event.getMessageAuthor().getDisplayName();
    String username = event.getMessageAuthor().getName();
    this.connectionManager.markUserSeen(
        this,
        echoToAlias,
        userId,
        username,
        displayName == null || displayName.isBlank() ? (isPrivate ? "Discord DM " + username : username) : displayName,
        "DISCORD_MESSAGE",
        isPrivate ? event.getChannel().getIdAsString() : null,
        isPrivate ? "Discord DM " + username : null);
  }

  private org.freakz.common.model.botconfig.Channel resolveConfiguredChannel(MessageCreateEvent event) {
    String channelName = event.getChannel().asServerChannel()
        .map(serverChannel -> serverChannel.getName())
        .orElseGet(() -> event.getChannel().asPrivateChannel().isPresent() ? event.getChannel().asPrivateChannel().get().getIdAsString() : null);
    if (channelName == null) {
      return null;
    }
    for (org.freakz.common.model.botconfig.Channel channel : config.getChannelList()) {
      if (channelName.equalsIgnoreCase(channel.getName())) {
        return channel;
      }
    }
    return null;
  }

  private String clean(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }


}

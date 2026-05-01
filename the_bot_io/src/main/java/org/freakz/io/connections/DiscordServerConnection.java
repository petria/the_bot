package org.freakz.io.connections;

import org.freakz.common.exception.BotIOException;
import org.freakz.common.exception.InvalidEchoToAliasException;
import org.freakz.common.model.botconfig.DiscordConfig;
import org.freakz.common.model.feed.Message;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.Channel;
import org.javacord.api.entity.channel.PrivateChannel;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.MessageAttachment;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.message.MessageCreateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Set;

public class DiscordServerConnection extends BotConnection {

  private static final Logger log = LoggerFactory.getLogger(DiscordServerConnection.class);

  private final EventPublisher publisher;
  private DiscordApi api;
  private ConnectionManager connectionManager;
  private DiscordConfig config;

  public DiscordServerConnection(EventPublisher publisher) {
    super(BotConnectionType.DISCORD_CONNECTION);
    this.publisher = publisher;
  }

  @Override
  public String getNetwork() {
    return "Discord";
  }

  public void init(ConnectionManager connectionManager, DiscordConfig config) {

    this.connectionManager = connectionManager;
    this.config = config;

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
        .setToken(token)
        .setWaitForServersOnStartup(false)
        .login()
        .join();

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
  public void sendMessageTo(Message message) {
    Channel channel = null;
    Set<Channel> channels = api.getChannels();
    for (Channel ch : channels) {
/*            if (ch.asVoiceChannel().isPresent()) {
                continue;
            }
            */
      String chId = "" + ch.getId();
      if (chId.equals(message.getId())) {
        channel = ch;
        break;
      }
      String t = ch.toString();
      if (t.contains("name: " + message.getTarget())) {
        channel = ch;
        break;
      }
    }
    if (channel != null) {
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
    if (messageAuthor.asUser().isPresent()
        && messageAuthor.asUser().get().getId() == this.config.getTheBotUserId()) {
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

    TextChannel discordChannel = event.getChannel();

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
      checkEchoTo(this.config, this.connectionManager, channelName, event.getMessageAuthor().getName(), messageTxt.toString());
    }
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

  protected void checkEchoTo(DiscordConfig config, ConnectionManager connectionManager, String channelName, String actorName, String message) {
    String name = channelName; //event.getChannel().getName();
    config.getChannelList().forEach(ch -> {
      if (ch.getName().equals(name)) {
        if (ch.getEchoToAliases() != null && ch.getEchoToAliases().size() > 0) {
          for (String echoToAlias : ch.getEchoToAliases()) {
            log.debug("Echo to: {}", echoToAlias);
            try {
              if (!message.startsWith("!")) {
                //                                    String msg = String.format("%s%s<%s@Telegram>: %s", "\u0002", "\u0002", actorName, message);
                String msg = String.format("<%s@Dicord>: %s", actorName, message);
                connectionManager.sendMessageByEchoToAlias(msg, echoToAlias);
              }
            } catch (InvalidEchoToAliasException e) {
              log.error("Can not echo message to: {}", echoToAlias);
            }
          }
        }
      }
    });
  }


}

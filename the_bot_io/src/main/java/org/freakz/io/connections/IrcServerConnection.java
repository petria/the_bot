package org.freakz.io.connections;

import net.engio.mbassy.listener.Handler;
import org.freakz.common.chat.ChatIdentityUtil;
import org.freakz.common.chat.BotSelfIdentity;
import org.freakz.common.exception.BotIOException;
import org.freakz.common.model.botconfig.IrcServerConfig;
import org.freakz.common.model.botconfig.TheBotConfig;
import org.freakz.common.model.connectionmanager.ChannelUser;
import org.freakz.common.model.feed.Message;
import org.freakz.common.model.feed.MessageSource;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.element.mode.ChannelUserMode;
import org.kitteh.irc.client.library.event.channel.*;
import org.kitteh.irc.client.library.event.client.ClientNegotiationCompleteEvent;
import org.kitteh.irc.client.library.event.connection.ClientConnectionClosedEvent;
import org.kitteh.irc.client.library.event.connection.ClientConnectionEndedEvent;
import org.kitteh.irc.client.library.event.connection.ClientConnectionEstablishedEvent;
import org.kitteh.irc.client.library.event.user.PrivateMessageEvent;
import org.kitteh.irc.client.library.event.user.UserNickChangeEvent;
import org.kitteh.irc.client.library.event.user.UserQuitEvent;
import org.kitteh.irc.client.library.event.user.WhoisEvent;
import org.kitteh.irc.client.library.util.Cutter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class IrcServerConnection extends BotConnection {

  private static final Logger log = LoggerFactory.getLogger(IrcServerConnection.class);

  private final EventPublisher publisher;
  private final Queue<WhoisEvent> whoisEventQueue = new ConcurrentLinkedQueue<>();
  private Client client;
  private ConnectionManager connectionManager;
  private IrcServerConfig config;
  private String botNick;
  private volatile boolean intentionalStop;

  public IrcServerConnection(EventPublisher publisher) {
    super(BotConnectionType.IRC_CONNECTION);
    this.publisher = publisher;
  }

  public IrcServerConfig getConfig() {
    return config;
  }

  public Client getClient() {
    return client;
  }

  @Override
  public String getNetwork() {
    return config.getIrcNetwork().getName();
  }

  @Handler
  public void onUserJoinChannel(ChannelJoinEvent event) throws BotIOException {
    updateChannelMap(event.getChannel().getName());
    org.freakz.common.model.botconfig.Channel channel = resolveByEchoTo(event.getChannel().getName());
    if (channel != null) {
      markIrcUserSeen(channel.getEchoToAlias(), event.getChannel(), event.getUser(), "IRC_JOIN");
    }
    if (event.getClient().isUser(event.getUser())) { // It's me!
//            event.getChannel().sendMessage("Hello world! Kitteh's here for cuddles.");
      return;
    }
    // It's not me!
//        event.getChannel().sendMessage("Welcome, " + event.getUser().getNick() + "! :3");
  }

  @Handler
  public void onChannelPartEvent(ChannelPartEvent event) {
    if (event.getClient().isUser(event.getUser())) {
      log.debug("Parted: {}", event);
      return;
    }
    org.freakz.common.model.botconfig.Channel channel = resolveByEchoTo(event.getChannel().getName());
    if (channel != null) {
      this.connectionManager.removeUserFromChannel(
          this,
          channel.getEchoToAlias(),
          event.getUser().getNick(),
          event.getUser().getNick(),
          event.getUser().getRealName().orElse(null));
    }
  }

  @Handler
  public void onChannelKickEvent(ChannelKickEvent event) {
    org.freakz.common.model.botconfig.Channel channel = resolveByEchoTo(event.getChannel().getName());
    if (channel != null) {
      removeIrcUserSeen(channel.getEchoToAlias(), event.getTarget());
    }
  }

  @Handler
  public void onUserQuitEvent(UserQuitEvent event) {
    event.getAffectedChannel().ifPresent(channel -> {
      org.freakz.common.model.botconfig.Channel configuredChannel = resolveByEchoTo(channel.getName());
      if (configuredChannel != null) {
        removeIrcUserSeen(configuredChannel.getEchoToAlias(), event.getUser());
      }
    });
  }

  @Handler
  public void onUserNickChangeEvent(UserNickChangeEvent event) {
    for (String channelName : event.getNewUser().getChannels()) {
      org.freakz.common.model.botconfig.Channel channel = resolveByEchoTo(channelName);
      if (channel != null) {
        removeIrcUserSeen(channel.getEchoToAlias(), event.getOldUser());
        client.getChannel(channelName).ifPresentOrElse(
            ircChannel -> markIrcUserSeen(channel.getEchoToAlias(), ircChannel, event.getNewUser(), "IRC_NICK"),
            () -> markIrcUserSeen(channel.getEchoToAlias(), event.getNewUser(), "IRC_NICK"));
      }
    }
  }

  @Handler
  public void onChannelUsersUpdatedEvent(ChannelUsersUpdatedEvent event) throws BotIOException {
    String channelName = event.getChannel().getName();
    log.debug("onChannelUsersUpdatedEvent: {}", channelName);
    updateChannelMap(channelName);
    List<User> users = event.getChannel().getUsers();
    for (User user : users) {
      log.debug("{} -> user -> {}", channelName, user.toString());
      org.freakz.common.model.botconfig.Channel channel = resolveByEchoTo(channelName);
      if (channel != null) {
        markIrcUserSeen(channel.getEchoToAlias(), event.getChannel(), user, "IRC_NAMES");
      }
    }
  }

  private void updateChannelMap(String channelName) throws BotIOException {

    org.freakz.common.model.botconfig.Channel channel = resolveByEchoTo(channelName);
    if (channel == null) {
      throw new BotIOException("No Channel config found with: " + channelName);
    }

    JoinedChannelContainer container = this.connectionManager.getJoinedChannelContainer(channel.getEchoToAlias());
    BotConnectionChannel botConnectionChannel;
    if (container == null) {
      botConnectionChannel = new BotConnectionChannel();
      botConnectionChannel.setName(channel.getName());
      botConnectionChannel.setId(channel.getId());
      botConnectionChannel.setType(getType().name());
      botConnectionChannel.setNetwork(getNetwork());
      botConnectionChannel.setEchoToAlias(channel.getEchoToAlias());

    } else {
      botConnectionChannel = container.channel;
    }
    botConnectionChannel.setConfigured(true);

    this.connectionManager.updateJoinedChannelsMap(BotConnectionType.IRC_CONNECTION, this, botConnectionChannel);

    log.debug("Updated channel: {}", botConnectionChannel);
  }

  private org.freakz.common.model.botconfig.Channel resolveByEchoTo(String channelName) {
    for (org.freakz.common.model.botconfig.Channel channel : this.config.getChannelList()) {
      if (channel.getName().equalsIgnoreCase(channelName)) {
        return channel;
      }
    }
    return null;
  }

  @Handler
  public void onPrivateMessageEvent(PrivateMessageEvent event) {
    log.debug("Got private msg: {}", event.getMessage());
    String echoToAlias = "PRIVATE-" + event.getActor().getNick();
    this.connectionManager.markMessageReceived(echoToAlias, event.getActor().getNick(), "IRC");
    markIrcUserSeen(echoToAlias, event.getActor(), "IRC_PRIVATE_MESSAGE");
    publisher.publishEvent(this, event, echoToAlias);
  }

  @Handler
  public void onChannelMessageEvent(ChannelMessageEvent event) throws BotIOException {
    log.debug("Got channel msg: {}", event.getMessage());
    org.freakz.common.model.botconfig.Channel channel = resolveByEchoTo(event.getChannel().getName());
    String echoToAlias = null;
    if (channel != null) {
      echoToAlias = channel.getEchoToAlias();
    }
    this.connectionManager.markMessageReceived(echoToAlias, event.getActor().getNick(), "IRC");
    markIrcUserSeen(echoToAlias, event.getChannel(), event.getActor(), "IRC_MESSAGE");
    publisher.publishEvent(this, event, echoToAlias);
    updateChannelMap(event.getChannel().getName());
    BridgeEchoService.echoToConfiguredTargets(
        this.connectionManager,
        channel,
        "IRC",
        event.getActor().getNick(),
        event.getMessage(),
        botNick);
  }

  private void markIrcUserSeen(String echoToAlias, User user, String source) {
    markIrcUserSeen(echoToAlias, null, user, source);
  }

  private void markIrcUserSeen(String echoToAlias, Channel ircChannel, User user, String source) {
    if (user == null) {
      return;
    }
    IrcChannelModeMetadata modeMetadata = ircModeMetadata(ircChannel, user);
    this.connectionManager.markUserSeen(
        this,
        echoToAlias,
        user.getNick(),
        user.getNick(),
        user.getRealName().orElse(null),
        source,
        null,
        null,
        modeMetadata.displayPrefix(),
        modeMetadata.channelModes(),
        List.of());
  }

  private void removeIrcUserSeen(String echoToAlias, User user) {
    if (user == null) {
      return;
    }
    this.connectionManager.removeUserFromChannel(
        this,
        echoToAlias,
        user.getNick(),
        user.getNick(),
        user.getRealName().orElse(null));
  }

  @Handler
  public void handleConnectionEstablished(ClientConnectionEstablishedEvent event) {
    this.connectionManager.ircConnectionEstablished(this);
    this.connectionManager.removeConfiguredIrcJoinedChannels(this);
  }

  @Handler
  public void handleNegotiationComplete(ClientNegotiationCompleteEvent event) {
    joinConfiguredChannels();
  }

  @Handler
  public void handleConnectionEnded(ClientConnectionEndedEvent event) {
    if (event instanceof ClientConnectionClosedEvent closedEvent) {
      log.debug(
          ">> ENDED, shutting down this client; canReconnect={}, willReconnect={}, lastMessage={}",
          event.canAttemptReconnect(),
          event.willAttemptReconnect(),
          closedEvent.getLastMessage().orElse("")
      );
    } else {
      log.debug(
          ">> ENDED, shutting down this client; canReconnect={}, willReconnect={}, cause={}",
          event.canAttemptReconnect(),
          event.willAttemptReconnect(),
          event.getCause().map(Throwable::toString).orElse("")
      );
    }
    event.setAttemptReconnect(false);
    this.connectionManager.ircConnectionEnded(this, intentionalStop);
    this.client.shutdown();
  }

  @Override
  public void stop() {
    intentionalStop = true;
    if (client != null) {
      client.shutdown();
    }
  }

  @Override
  public synchronized void applyChannelConfig(TheBotConfig theBotConfig) {
    if (theBotConfig == null || theBotConfig.getIrcServerConfigs() == null || config == null) {
      return;
    }
    for (IrcServerConfig candidate : theBotConfig.getIrcServerConfigs()) {
      if (same(candidate.getName(), config.getName())) {
        applyConfig(candidate);
        return;
      }
    }
  }

  public synchronized void applyConfig(IrcServerConfig newConfig) {
    if (newConfig == null) {
      return;
    }
    IrcServerConfig oldConfig = this.config;
    this.config = newConfig;
    applyJoinOnStartChanges(oldConfig, newConfig);
  }

  public void init(ConnectionManager connectionManager, String botNick, String ircRealName, IrcServerConfig config) {
    this.connectionManager = connectionManager;
    this.config = config;
    this.botNick = botNick;
    setSelfIdentity(new BotSelfIdentity("irc", botNick, List.of(botNick)));

    client = Client.builder()
        .user("hokan")
        .nick(botNick)
        .realName(firstNonBlank(ircRealName, botNick, "the_bot"))
        .server()
        .host(config.getIrcNetwork().getIrcServer().getHost())
        .port(config.getIrcNetwork().getIrcServer().getPort(), Client.Builder.Server.SecurityType.INSECURE)
        .then()
        .listeners()
        .input(line -> log.debug("IRC << {}", line))
        .output(line -> log.debug("IRC >> {}", line))
        .exception(e -> log.warn("IRC client exception", e))
        .then()
        .build();

    client.getEventManager().registerEventListener(this);
    client.connect();

  }

  private String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value.trim();
      }
    }
    return null;
  }

  private void joinConfiguredChannels() {
    config.getChannelList().forEach(ch -> {
          if (ch.isJoinOnStart()) {
            log.debug("Join channel: {}", ch.getName());
            client.addChannel(ch.getName());
          } else {
            log.debug("Not join channel: {}", ch.getName());
          }
        }
    );
  }

  private void applyJoinOnStartChanges(IrcServerConfig oldConfig, IrcServerConfig newConfig) {
    Map<String, org.freakz.common.model.botconfig.Channel> oldByName = channelsByName(oldConfig);
    Map<String, org.freakz.common.model.botconfig.Channel> newByName = channelsByName(newConfig);

    for (org.freakz.common.model.botconfig.Channel oldChannel : oldByName.values()) {
      org.freakz.common.model.botconfig.Channel newChannel = newByName.get(normalizeChannelName(oldChannel.getName()));
      if (oldChannel.isJoinOnStart() && (newChannel == null || !newChannel.isJoinOnStart())) {
        partChannel(oldChannel);
      }
    }

    for (org.freakz.common.model.botconfig.Channel newChannel : newByName.values()) {
      org.freakz.common.model.botconfig.Channel oldChannel = oldByName.get(normalizeChannelName(newChannel.getName()));
      if (newChannel.isJoinOnStart() && (oldChannel == null || !oldChannel.isJoinOnStart())) {
        joinChannel(newChannel);
      }
    }
  }

  private Map<String, org.freakz.common.model.botconfig.Channel> channelsByName(IrcServerConfig config) {
    Map<String, org.freakz.common.model.botconfig.Channel> channels = new HashMap<>();
    if (config == null || config.getChannelList() == null) {
      return channels;
    }
    for (org.freakz.common.model.botconfig.Channel channel : config.getChannelList()) {
      String key = normalizeChannelName(channel.getName());
      if (key != null) {
        channels.put(key, channel);
      }
    }
    return channels;
  }

  private void joinChannel(org.freakz.common.model.botconfig.Channel channel) {
    if (client == null || channel == null || channel.getName() == null || channel.getName().isBlank()) {
      return;
    }
    log.debug("Hot-joining IRC channel after config apply: {}", channel.getName());
    client.addChannel(channel.getName());
  }

  private void partChannel(org.freakz.common.model.botconfig.Channel channel) {
    if (client == null || channel == null || channel.getName() == null || channel.getName().isBlank()) {
      return;
    }
    log.debug("Hot-parting IRC channel after config apply: {}", channel.getName());
    client.removeChannel(channel.getName(), "configuration updated");
    if (connectionManager != null) {
      connectionManager.removeJoinedChannelForConnection(channel.getEchoToAlias(), this);
    }
  }

  private String normalizeChannelName(String channelName) {
    if (channelName == null || channelName.isBlank()) {
      return null;
    }
    return channelName.trim().toLowerCase();
  }

  private boolean same(String left, String right) {
    if (left == null || right == null) {
      return left == right;
    }
    return left.equalsIgnoreCase(right);
  }

  @Override
  public void sendMessageTo(Message message) {
    String nick = null;
    if (message.getTarget().startsWith("PRIVATE-")) {
      nick = message.getTarget().replaceFirst("PRIVATE-", "");
    }

    Optional<Channel> channel = client.getChannel(message.getTarget());
    if (channel.isPresent() || nick != null) {
      Cutter messageCutter = client.getMessageCutter();
      String[] logicalLines = message.getMessage().split("\\R", -1);
      for (String logicalLine : logicalLines) {
        if (logicalLine.isBlank()) {
          continue;
        }
        for (String splitLine : messageCutter.split(logicalLine, 400)) {
          if (nick != null) {
            client.sendMessage(nick, splitLine);
          } else {
            channel.get().sendMessage(splitLine);
            String protocol = "irc";
            String network = ChatIdentityUtil.sanitize(getNetwork(), "unknown");
            String chatType = message.getTarget().startsWith("PRIVATE-") ? "dm" : "channel";
            String target = ChatIdentityUtil.sanitize(message.getTarget().replaceFirst("^PRIVATE-", ""), "unknown");
            publisher.logMessage(MessageSource.NONE, protocol, network + "/" + chatType + "/" + target, botNick, splitLine);
          }
        }
      }
    } else {
      log.error("Can't send message to: {}", message.getTarget());
    }
  }

  @Override
  public void sendRawMessage(Message message) {
    log.debug("Send raw message: '{}'", message.getMessage());
    client.sendRawLineImmediately(message.getMessage());
  }

  @Override
  public List<ChannelUser> getChannelUsersByEchoToAlias(String echoToAlias, BotConnectionChannel channel) {
//        List<String> userList = new ArrayList<>();
    List<ChannelUser> channelUsers = new ArrayList<>();
    Optional<Channel> optional = client.getChannel(channel.getName());
    if (optional.isPresent()) {
      Channel ircChannel = optional.get();
      List<User> ircUsers = ircChannel.getUsers();
      for (User user : ircUsers) {
        IrcChannelModeMetadata modeMetadata = ircModeMetadata(ircChannel, user);
        ChannelUser channelUser
            = ChannelUser.builder()
            .account(user.getAccount().orElse(""))
            .awayMessage(user.getAwayMessage().orElse(""))
            .host(user.getHost())
            .nick(user.getNick())
            .operatorInformation(user.getOperatorInformation().orElse(""))
            .realName(user.getRealName().orElse(""))
            .server(user.getServer().orElse(""))
            .userString(user.getUserString())
            .displayPrefix(modeMetadata.displayPrefix())
            .channelModes(modeMetadata.channelModes())
            .channelRoles(List.of())
            .isAway(user.isAway())
            .build();
        channelUsers.add(channelUser);

//                userList.add(user.getNick() + " : " + user.getName());
      }
    }
    return channelUsers;
  }

  private IrcChannelModeMetadata ircModeMetadata(Channel ircChannel, User user) {
    if (ircChannel == null || user == null) {
      return IrcChannelModeMetadata.empty();
    }
    Optional<SortedSet<ChannelUserMode>> modes = ircChannel.getUserModes(user);
    if (modes.isEmpty()) {
      return IrcChannelModeMetadata.empty();
    }
    List<String> prefixes = modes.get().stream()
        .map(ChannelUserMode::getNickPrefix)
        .map(String::valueOf)
        .filter(prefix -> !prefix.isBlank())
        .distinct()
        .toList();
    return prefixes.isEmpty()
        ? IrcChannelModeMetadata.empty()
        : new IrcChannelModeMetadata(String.join("", prefixes), prefixes);
  }

  private record IrcChannelModeMetadata(String displayPrefix, List<String> channelModes) {
    static IrcChannelModeMetadata empty() {
      return new IrcChannelModeMetadata(null, List.of());
    }
  }

  @Handler
  public void handleWhoisReply(WhoisEvent event) {
    log.debug("whois - {}", event);
    synchronized (whoisEventQueue) {
      whoisEventQueue.add(event);
      log.debug(">>> whoisEventQueue.size() = {}", whoisEventQueue.size());
      whoisEventQueue.notify();
    }
    int foo = 0;
  }

  public WhoisEvent sendSyncWhois(String whois, long maxWaitTimeout) throws InterruptedException {

    whoisEventQueue.clear();
    log.debug("send raw");
    client.sendRawLine(whois);
    log.debug("send raw done");
//        client.sendRawLineImmediately(whois);
    synchronized (whoisEventQueue) {
      log.debug("start wait");
      whoisEventQueue.wait();
      log.debug("wait done");

      WhoisEvent whoisEvent = whoisEventQueue.remove();
      log.debug("Got event from queue: {}", whoisEvent);
      return whoisEvent;
    }

  }

  @Override
  public String toString() {
    return "IrcServerConnection{botNick: " + botNick + ", config { name: " + config.getName() + "}}";
  }


}

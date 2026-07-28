package org.freakz.io.connections;

import net.engio.mbassy.listener.Handler;
import org.freakz.common.chat.ChatIdentityUtil;
import org.freakz.common.chat.BotSelfIdentity;
import org.freakz.common.exception.BotIOException;
import org.freakz.common.spring.rest.RestEngineClient;
import org.freakz.common.model.botconfig.IrcServerConfig;
import org.freakz.common.model.botconfig.TheBotConfig;
import org.freakz.common.model.connectionmanager.ChannelUser;
import org.freakz.common.model.connectionmanager.IrcOperatorReconcileRequest;
import org.freakz.common.model.connectionmanager.IrcOperatorReconcileResponse;
import org.freakz.common.model.connectionmanager.IrcOperatorStateResponse;
import org.freakz.common.model.connectionmanager.IrcOperatorGrantRequest;
import org.freakz.common.model.connectionmanager.IrcOperatorGrantResponse;
import org.freakz.common.model.connectionmanager.IrcOperatorModeRequest;
import org.freakz.common.model.connectionmanager.IrcOperatorModeResponse;
import org.freakz.common.model.connectionmanager.IrcChannelControlRequest;
import org.freakz.common.model.connectionmanager.IrcChannelControlResponse;
import org.freakz.common.model.feed.Message;
import org.freakz.common.model.feed.MessageSource;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.element.mode.ChannelUserMode;
import org.kitteh.irc.client.library.element.mode.ModeStatus;
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
import java.util.concurrent.CompletableFuture;

@Service
public class IrcServerConnection extends BotConnection {

  private static final Logger log = LoggerFactory.getLogger(IrcServerConnection.class);

  private final EventPublisher publisher;
  @org.springframework.beans.factory.annotation.Autowired(required = false)
  private RestEngineClient engineClient;
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

  public IrcOperatorStateResponse getOperatorState(String echoToAlias) {
    org.freakz.common.model.botconfig.Channel configured = resolveConfiguredEchoAlias(echoToAlias);
    if (configured == null || client == null) {
      return new IrcOperatorStateResponse(echoToAlias, null, false, List.of());
    }
    Optional<Channel> optional = client.getChannel(configured.getName());
    if (optional.isEmpty()) {
      return new IrcOperatorStateResponse(configured.getEchoToAlias(), configured.getName(), false, List.of());
    }
    Channel channel = optional.get();
    Optional<User> botUser = channel.getUser(botNick);
    boolean botHasOperator = botUser.map(user -> hasMode(channel, user, 'o')).orElse(false);
    return new IrcOperatorStateResponse(
        configured.getEchoToAlias(),
        configured.getName(),
        botHasOperator,
        channelUsers(channel));
  }

  public IrcOperatorReconcileResponse reconcileOperators(IrcOperatorReconcileRequest request) {
    IrcOperatorStateResponse state = getOperatorState(request == null ? null : request.echoToAlias());
    if (!state.botHasOperator()) {
      return new IrcOperatorReconcileResponse(state.echoToAlias(), false, List.of(), List.of(), "Bot is not an IRC channel operator");
    }
    Optional<ChannelUserMode> operatorMode = ChannelUserMode.get(client, 'o');
    if (operatorMode.isEmpty()) {
      return new IrcOperatorReconcileResponse(state.echoToAlias(), true, List.of(), List.of(), "IRC operator mode is unavailable");
    }
    List<String> authorized = request == null || request.nicks() == null ? List.of() : request.nicks();
    Optional<Channel> optional = client.getChannel(state.channelName());
    if (optional.isEmpty()) {
      return new IrcOperatorReconcileResponse(state.echoToAlias(), true, List.of(), List.of(), "IRC channel is not joined");
    }
    Channel channel = optional.get();
    var command = channel.commands().mode();
    List<String> granted = new ArrayList<>();
    List<String> skipped = new ArrayList<>();
    for (User user : channel.getUsers()) {
      if (!containsNick(authorized, user.getNick())) {
        continue;
      }
      if (hasMode(channel, user, 'o')) {
        skipped.add(user.getNick());
      } else {
        command.add(ModeStatus.Action.ADD, operatorMode.get(), user);
        granted.add(user.getNick());
      }
    }
    if (!granted.isEmpty()) {
      command.execute();
    }
    return new IrcOperatorReconcileResponse(state.echoToAlias(), true, granted, skipped, null);
  }

  public IrcOperatorGrantResponse grantOperator(IrcOperatorGrantRequest request) {
    IrcOperatorStateResponse state = getOperatorState(request == null ? null : request.echoToAlias());
    if (!state.botHasOperator()) {
      return new IrcOperatorGrantResponse(state.echoToAlias(), false, false, false,
          "Bot is not an IRC channel operator");
    }
    if (request == null || request.nick() == null || request.nick().isBlank()) {
      return new IrcOperatorGrantResponse(state.echoToAlias(), true, false, false, "IRC nick is required");
    }
    Optional<ChannelUserMode> operatorMode = ChannelUserMode.get(client, 'o');
    if (operatorMode.isEmpty()) {
      return new IrcOperatorGrantResponse(state.echoToAlias(), true, false, false,
          "IRC operator mode is unavailable");
    }
    Optional<Channel> optional = client.getChannel(state.channelName());
    if (optional.isEmpty()) {
      return new IrcOperatorGrantResponse(state.echoToAlias(), true, false, false,
          "IRC channel is not joined");
    }
    Channel channel = optional.get();
    Optional<User> target = channel.getUsers().stream()
        .filter(user -> user.getNick().equalsIgnoreCase(request.nick()))
        .findFirst();
    if (target.isEmpty()) {
      return new IrcOperatorGrantResponse(state.echoToAlias(), true, false, false,
          "IRC nick is not present in the channel");
    }
    User user = target.get();
    if (hasMode(channel, user, 'o')) {
      return new IrcOperatorGrantResponse(state.echoToAlias(), true, false, true, null);
    }
    var command = channel.commands().mode();
    command.add(ModeStatus.Action.ADD, operatorMode.get(), user);
    command.execute();
    return new IrcOperatorGrantResponse(state.echoToAlias(), true, true, false, null);
  }

  public IrcOperatorModeResponse setOperatorMode(IrcOperatorModeRequest request) {
    IrcOperatorStateResponse state = getOperatorState(request == null ? null : request.echoToAlias());
    boolean operator = request != null && request.operator();
    if (!state.botHasOperator()) {
      return new IrcOperatorModeResponse(state.echoToAlias(), false, operator, List.of(), List.of(),
          "Bot is not an IRC channel operator");
    }
    List<String> requestedNicks = request == null || request.nicks() == null ? List.of() : request.nicks();
    List<String> nicks = requestedNicks.stream()
        .filter(nick -> nick != null && !nick.isBlank())
        .map(String::trim)
        .distinct()
        .toList();
    if (nicks.isEmpty()) {
      return new IrcOperatorModeResponse(state.echoToAlias(), true, operator, List.of(), List.of(), "IRC nick is required");
    }
    Optional<ChannelUserMode> operatorMode = ChannelUserMode.get(client, 'o');
    if (operatorMode.isEmpty()) {
      return new IrcOperatorModeResponse(state.echoToAlias(), true, operator, List.of(), List.of(),
          "IRC operator mode is unavailable");
    }
    Optional<Channel> optional = client.getChannel(state.channelName());
    if (optional.isEmpty()) {
      return new IrcOperatorModeResponse(state.echoToAlias(), true, operator, List.of(), List.of(), "IRC channel is not joined");
    }
    Channel channel = optional.get();
    var command = channel.commands().mode();
    List<String> changed = new ArrayList<>();
    List<String> unchanged = new ArrayList<>();
    for (String nick : nicks) {
      Optional<User> target = channel.getUsers().stream()
          .filter(user -> user.getNick().equalsIgnoreCase(nick))
          .findFirst();
      if (target.isEmpty() || hasMode(channel, target.get(), 'o') == operator) {
        unchanged.add(nick);
        continue;
      }
      command.add(operator ? ModeStatus.Action.ADD : ModeStatus.Action.REMOVE, operatorMode.get(), target.get());
      changed.add(target.get().getNick());
    }
    if (!changed.isEmpty()) {
      command.execute();
    }
    return new IrcOperatorModeResponse(state.echoToAlias(), true, operator, changed, unchanged, null);
  }

  public IrcChannelControlResponse controlChannel(IrcChannelControlRequest request) {
    org.freakz.common.model.botconfig.Channel configured =
        resolveConfiguredEchoAlias(request == null ? null : request.echoToAlias());
    String echoToAlias = configured == null ? request == null ? null : request.echoToAlias() : configured.getEchoToAlias();
    String channelName = configured == null ? null : configured.getName();
    String action = request == null || request.action() == null ? null : request.action().trim().toUpperCase();
    if (configured == null) {
      return new IrcChannelControlResponse(echoToAlias, channelName, action, false, false,
          "IRC channel is not configured");
    }
    if (client == null || action == null || (!"JOIN".equals(action) && !"PART".equals(action))) {
      return new IrcChannelControlResponse(echoToAlias, channelName, action, false, false,
          "IRC channel action is unavailable");
    }
    if ("JOIN".equals(action)) {
      if (client.getChannel(channelName).isPresent()) {
        return new IrcChannelControlResponse(echoToAlias, channelName, action, false, true, null);
      }
      client.addChannel(channelName);
      return new IrcChannelControlResponse(echoToAlias, channelName, action, true, true, null);
    }
    if (client.getChannel(channelName).isEmpty()) {
      return new IrcChannelControlResponse(echoToAlias, channelName, action, false, false, null);
    }
    client.removeChannel(channelName, "requested by IRC admin command");
    if (connectionManager != null) {
      connectionManager.removeJoinedChannelForConnection(echoToAlias, this);
    }
    return new IrcChannelControlResponse(echoToAlias, channelName, action, true, false, null);
  }

  private org.freakz.common.model.botconfig.Channel resolveConfiguredEchoAlias(String echoToAlias) {
    if (config == null || config.getChannelList() == null || echoToAlias == null) {
      return null;
    }
    return config.getChannelList().stream()
        .filter(channel -> channel.getEchoToAlias() != null && channel.getEchoToAlias().equalsIgnoreCase(echoToAlias))
        .findFirst()
        .orElse(null);
  }

  private boolean containsNick(List<String> nicks, String nick) {
    return nick != null && nicks.stream().anyMatch(value -> value != null && value.equalsIgnoreCase(nick));
  }

  private boolean hasMode(Channel channel, User user, char modeChar) {
    return channel.getUserModes(user).orElseGet(java.util.TreeSet::new).stream()
        .anyMatch(mode -> mode.getChar() == modeChar);
  }

  private List<ChannelUser> channelUsers(Channel channel) {
    List<ChannelUser> users = new ArrayList<>();
    for (User user : channel.getUsers()) {
      IrcChannelModeMetadata modeMetadata = ircModeMetadata(channel, user);
      users.add(ChannelUser.builder()
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
          .build());
    }
    return users;
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
      requestOperatorReconciliation(channel.getEchoToAlias());
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
    if (config == null || config.getChannelList() == null) {
      return;
    }
    // IRC QUIT removes the user from every channel on this connection. Kitteh
    // exposes only one optional affected channel, which is insufficient when
    // the user was present in several configured channels.
    config.getChannelList().stream()
        .map(channel -> resolveByEchoTo(channel.getName()))
        .filter(java.util.Objects::nonNull)
        .forEach(channel -> removeIrcUserSeen(channel.getEchoToAlias(), event.getUser()));
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
        requestOperatorReconciliation(channel.getEchoToAlias());
      }
    }
  }

  @Handler
  public void onChannelUsersUpdatedEvent(ChannelUsersUpdatedEvent event) throws BotIOException {
    String channelName = event.getChannel().getName();
    log.debug("onChannelUsersUpdatedEvent: {}", channelName);
    updateChannelMap(channelName);
    List<User> users = event.getChannel().getUsers();
    org.freakz.common.model.botconfig.Channel configured = resolveByEchoTo(channelName);
    if (configured != null) {
      connectionManager.reconcileIrcChannelUsers(
          this,
          configured.getEchoToAlias(),
          channelUsers(event.getChannel()));
    }
    for (User user : users) {
      log.debug("{} -> user -> {}", channelName, user.toString());
      if (configured != null) {
        markIrcUserSeen(configured.getEchoToAlias(), event.getChannel(), user, "IRC_NAMES");
      }
    }
    if (configured != null) {
      requestOperatorReconciliation(configured.getEchoToAlias());
    }
  }

  public void reconcileJoinedChannelUsers() {
    if (client == null || config == null || config.getChannelList() == null) {
      return;
    }
    client.getChannels().stream()
        .map(Channel::getName)
        .map(this::resolveByEchoTo)
        .filter(java.util.Objects::nonNull)
        .forEach(channel -> requestUserReconciliation(channel.getName()));
  }

  private void requestUserReconciliation(String channelName) {
    if (client == null || channelName == null || channelName.isBlank()) {
      return;
    }
    log.debug("Requesting IRC NAMES reconciliation for {}", channelName);
    client.sendRawLineAvoidingDuplication("NAMES " + channelName);
  }

  private void requestOperatorReconciliation(String echoToAlias) {
    org.freakz.common.model.botconfig.Channel configured = resolveConfiguredEchoAlias(echoToAlias);
    if (engineClient == null || configured == null || !Boolean.TRUE.equals(configured.getManageOperators())) {
      return;
    }
    CompletableFuture.runAsync(() -> {
      try {
        engineClient.reconcileIrcOperators(echoToAlias);
      } catch (RuntimeException e) {
        log.debug("IRC operator reconciliation request failed for {}: {}", echoToAlias, e.getMessage());
      }
    });
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
    if (newConfig.getChannelList() != null) {
      newConfig.getChannelList().stream()
          .filter(channel -> Boolean.TRUE.equals(channel.getManageOperators()))
          .forEach(channel -> requestOperatorReconciliation(channel.getEchoToAlias()));
    }
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

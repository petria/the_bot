package org.freakz.io.connections;


import org.freakz.common.exception.EchoToAliasNotIrcChannelException;
import org.freakz.common.exception.InvalidChannelIdException;
import org.freakz.common.exception.InvalidEchoToAliasException;
import org.freakz.common.model.botconfig.IrcServerConfig;
import org.freakz.common.model.botconfig.TheBotConfig;
import org.freakz.common.model.connectionmanager.ChannelUser;
import org.freakz.common.model.connectionmanager.KnownChatChannelResponse;
import org.freakz.common.model.connectionmanager.KnownChatUserResponse;
import org.freakz.common.model.connectionmanager.KnownUserTargetResponse;
import org.freakz.common.model.dto.UserValuesJsonContainer;
import org.freakz.common.model.feed.Message;
import org.freakz.common.model.feed.MessageSource;
import org.freakz.common.model.users.User;
import org.freakz.io.config.ConfigService;
import org.kitteh.irc.client.library.event.user.WhoisEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import tools.jackson.databind.json.JsonMapper;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ConnectionManager implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(ConnectionManager.class);
  private final Map<Integer, BotConnection> connectionMap = new ConcurrentHashMap<>();
  @Autowired
  private ConfigService configService;
  @Autowired
  private EventPublisher eventPublisher;
  @Autowired(required = false)
  private JsonMapper objectMapper;
  private final Map<String, JoinedChannelContainer> joinedChannelsMap = new ConcurrentHashMap<>();

  private final Map<String, LastChannelActivity> lastReceivedMessageByEchoToAlias = new ConcurrentHashMap<>();
  private final Map<String, KnownChannel> knownChannelsByAlias = new ConcurrentHashMap<>();
  private final Map<String, KnownUserPresence> knownUsersByUserAndChannel = new ConcurrentHashMap<>();
  private volatile List<User> configuredUsers = List.of();
  private volatile long configuredUsersLastModified = -1L;
  private volatile boolean configuredUsersInjectedForTesting = false;

  public void updateJoinedChannelsMap(BotConnectionType botConnectionType, BotConnection connection, BotConnectionChannel channel) {
    String normalizedEchoToAlias = normalizeEchoToAlias(channel == null ? null : channel.getEchoToAlias());
    if (normalizedEchoToAlias == null) {
      log.warn("Ignoring channel without echoToAlias: {}", channel);
      return;
    }
    JoinedChannelContainer container = joinedChannelsMap.get(normalizedEchoToAlias);
    if (container == null) {
      container = new JoinedChannelContainer();
    }
    container.botConnectionType = botConnectionType;
    container.channel = channel;
    container.connection = connection;
    joinedChannelsMap.put(normalizedEchoToAlias, container);
    KnownChannel previous = knownChannelsByAlias.get(normalizedEchoToAlias);
    KnownChannel knownChannel = KnownChannel.from(botConnectionType, connection, channel);
    if (previous != null) {
      knownChannel.lastReceivedMessageAt = previous.lastReceivedMessageAt;
      knownChannel.lastReceivedMessageBy = previous.lastReceivedMessageBy;
      knownChannel.lastReceivedMessageSource = previous.lastReceivedMessageSource;
    }
    knownChannelsByAlias.put(normalizedEchoToAlias, knownChannel);
  }

  public void removeJoinedChannelsForConnection(BotConnection connection) {
    joinedChannelsMap.entrySet().removeIf(entry -> entry.getValue() != null && entry.getValue().connection == connection);
    knownChannelsByAlias.entrySet().removeIf(entry -> entry.getValue().connectionId == connection.getId());
    knownUsersByUserAndChannel.entrySet().removeIf(entry -> entry.getValue().connectionId == connection.getId());
  }

  public void removeConfiguredIrcJoinedChannels(IrcServerConnection connection) {
    IrcServerConfig config = connection.getConfig();
    if (config == null || config.getChannelList() == null) {
      return;
    }
    for (org.freakz.common.model.botconfig.Channel channel : config.getChannelList()) {
      if (channel.getEchoToAlias() == null) {
        continue;
      }
      JoinedChannelContainer container = joinedChannelsMap.get(normalizeEchoToAlias(channel.getEchoToAlias()));
      if (container != null
          && container.botConnectionType == BotConnectionType.IRC_CONNECTION
          && container.connection != connection) {
        String normalizedEchoToAlias = normalizeEchoToAlias(channel.getEchoToAlias());
        joinedChannelsMap.remove(normalizedEchoToAlias);
        knownChannelsByAlias.remove(normalizedEchoToAlias);
      }
    }
  }

  public Map<String, JoinedChannelContainer> getJoinedChannelsMap() {
    return this.joinedChannelsMap;
  }

  public JoinedChannelContainer getJoinedChannelContainer(String echoToAlias) {
    return joinedChannelsMap.get(normalizeEchoToAlias(echoToAlias));
  }

  public void markMessageReceived(String echoToAlias, String actor, String source) {
    markMessageReceived(echoToAlias, actor, source, null, null, null);
  }

  public void markMessageReceived(
      String echoToAlias,
      String actor,
      String source,
      String type,
      String network,
      String name) {
    String normalizedEchoToAlias = normalizeEchoToAlias(echoToAlias);
    if (normalizedEchoToAlias == null) {
      return;
    }
    lastReceivedMessageByEchoToAlias.put(
        normalizedEchoToAlias,
        new LastChannelActivity(System.currentTimeMillis(), actor, source, type, network, name)
    );
    KnownChannel channel = knownChannelsByAlias.get(normalizedEchoToAlias);
    if (channel != null) {
      channel.lastReceivedMessageAt = System.currentTimeMillis();
      channel.lastReceivedMessageBy = actor;
      channel.lastReceivedMessageSource = source;
    }
  }

  public void markUserSeen(
      BotConnection connection,
      String echoToAlias,
      String userId,
      String username,
      String displayName,
      String source) {
    String normalizedEchoToAlias = normalizeEchoToAlias(echoToAlias);
    if (connection == null || normalizedEchoToAlias == null) {
      return;
    }

    JoinedChannelContainer joinedChannel = joinedChannelsMap.get(normalizedEchoToAlias);
    BotConnectionChannel channel = joinedChannel == null ? null : joinedChannel.channel;
    if (channel == null) {
      channel = syntheticChannel(connection, echoToAlias);
      updateJoinedChannelsMap(connection.getType(), connection, channel);
    }

    String userKey = normalizeUserKey(connection.getType(), connection.getNetwork(), userId, username, displayName);
    if (userKey == null) {
      return;
    }

    knownUsersByUserAndChannel.put(
        userKey + "|" + normalizedEchoToAlias,
        KnownUserPresence.from(
            userKey,
            userId,
            username,
            displayName,
            connection,
            channel,
            System.currentTimeMillis(),
            source)
    );
  }

  public void removeUserFromChannel(BotConnection connection, String echoToAlias, String userId, String username, String displayName) {
    String normalizedEchoToAlias = normalizeEchoToAlias(echoToAlias);
    String userKey = normalizeUserKey(
        connection == null ? null : connection.getType(),
        connection == null ? null : connection.getNetwork(),
        userId,
        username,
        displayName);
    if (normalizedEchoToAlias == null || userKey == null) {
      return;
    }
    knownUsersByUserAndChannel.remove(userKey + "|" + normalizedEchoToAlias);
  }

  public List<org.freakz.common.model.connectionmanager.ChannelActivityResponse> getChannelActivity() {
    Map<String, org.freakz.common.model.connectionmanager.ChannelActivityResponse> channels = new ConcurrentHashMap<>();
    for (Map.Entry<String, JoinedChannelContainer> entry : joinedChannelsMap.entrySet()) {
      JoinedChannelContainer container = entry.getValue();
      if (container == null || container.channel == null || container.channel.getEchoToAlias() == null) {
        continue;
      }
      String normalizedEchoToAlias = normalizeEchoToAlias(container.channel.getEchoToAlias());
      LastChannelActivity activity = lastReceivedMessageByEchoToAlias.get(normalizedEchoToAlias);
      channels.put(normalizedEchoToAlias, org.freakz.common.model.connectionmanager.ChannelActivityResponse.builder()
          .echoToAlias(container.channel.getEchoToAlias())
          .type(container.channel.getType())
          .network(container.channel.getNetwork())
          .name(container.channel.getName())
          .lastReceivedMessageAt(activity == null ? null : activity.timestamp())
          .lastReceivedMessageBy(activity == null ? null : activity.actor())
          .lastReceivedMessageSource(activity == null ? null : activity.source())
          .build());
    }
    for (Map.Entry<String, LastChannelActivity> entry : lastReceivedMessageByEchoToAlias.entrySet()) {
      if (channels.containsKey(entry.getKey())) {
        continue;
      }
      LastChannelActivity activity = entry.getValue();
      channels.put(entry.getKey(), org.freakz.common.model.connectionmanager.ChannelActivityResponse.builder()
          .echoToAlias(entry.getKey())
          .type(activity.type())
          .network(activity.network())
          .name(activity.name() == null || activity.name().isBlank() ? entry.getKey() : activity.name())
          .lastReceivedMessageAt(activity.timestamp())
          .lastReceivedMessageBy(activity.actor())
          .lastReceivedMessageSource(activity.source())
          .build());
    }
    return new ArrayList<>(channels.values());
  }

  public List<KnownChatChannelResponse> getKnownChannels() {
    return knownChannelsByAlias.values().stream()
        .sorted(Comparator.comparing(KnownChannel::sortKey))
        .map(KnownChannel::toResponse)
        .toList();
  }

  public List<KnownChatUserResponse> getKnownUsers() {
    return knownUsersByUserAndChannel.values().stream()
        .sorted(Comparator.comparing(KnownUserPresence::sortKey))
        .map(KnownUserPresence::toResponse)
        .toList();
  }

  public List<KnownChatUserResponse> findKnownUsers(String query) {
    String normalizedQuery = normalizeLookup(query);
    if (normalizedQuery == null) {
      return getKnownUsers();
    }
    return knownUsersByUserAndChannel.values().stream()
        .filter(user -> user.matches(normalizedQuery))
        .sorted(Comparator.comparing(KnownUserPresence::sortKey))
        .map(KnownUserPresence::toResponse)
        .toList();
  }

  public List<KnownUserTargetResponse> getKnownUserTargets() {
    return findKnownUserTargets(null);
  }

  public List<KnownUserTargetResponse> findKnownUserTargets(String query) {
    String normalizedQuery = normalizeLookup(query);
    List<User> users = readConfiguredUsers();
    return knownUsersByUserAndChannel.values().stream()
        .map(presence -> resolveKnownUserTarget(presence, users))
        .filter(target -> normalizedQuery == null || target.matches(normalizedQuery))
        .sorted(Comparator.comparing(KnownUserTarget::sortKey))
        .map(KnownUserTarget::toResponse)
        .toList();
  }

  void setConfiguredUsersForTesting(List<User> users) {
    configuredUsers = users == null ? List.of() : new CopyOnWriteArrayList<>(users);
    configuredUsersInjectedForTesting = true;
  }

  public void addConnection(BotConnection connection) {
    this.connectionMap.put(connection.getId(), connection);
  }

  @Override
  public void run(String... args) throws Exception {
    log.debug(">>> Initializing connection manager <<<");
    init();
    log.debug(">>> Init done <<<");
  }

  @PostConstruct
  public void init() throws IOException, TelegramApiException {

    TheBotConfig theBotConfig = configService.readBotConfig();
    log.debug(">> Connecting IRC");
    for (IrcServerConfig config : theBotConfig.getIrcServerConfigs()) {
      log.debug("init IrcServerConfig: {}", config);
      if (config.isConnectStartup()) {
        IrcServerConnection isc = new IrcServerConnection(this.eventPublisher);
        isc.init(this, theBotConfig.getBotConfig().getBotName(), config);
      } else {
        log.warn("IRC Startup connect disabled: {}", config);
      }
    }
    log.debug("<< done!");

    log.debug(">> Connecting DISCORD");
    if (theBotConfig.getDiscordConfig().isConnectStartup()) {
      DiscordServerConnection dsc = new DiscordServerConnection(this.eventPublisher);
      dsc.init(this, theBotConfig.getDiscordConfig());
      addConnection(dsc);
    } else {
      log.warn("DISCORD Startup connect disabled: {}", theBotConfig.getDiscordConfig());
    }
    log.debug(">> done!");

    log.debug(">> Connecting TELEGRAM");
    if (theBotConfig.getTelegramConfig().isConnectStartup()) {
      TelegramConnection tc = new TelegramConnection(this.eventPublisher);
      tc.init(this, theBotConfig.getBotConfig().getBotName(), theBotConfig.getTelegramConfig());
      addConnection(tc);
    } else {
      log.warn("TELEGRAM Startup connect disabled: {}", theBotConfig.getTelegramConfig());
    }
    log.debug(">> done!");

  }


  public void reconnectIrcServer(IrcServerConfig config) {
    log.debug("Reconnecting IRC: {}", config);
    try {
      TheBotConfig theBotConfig = configService.readBotConfig();

      long waitTime = 10000L;
      log.debug("Reconnect wait time: {}", waitTime);
      Thread.sleep(waitTime);
      log.debug("Try reconnect: {}", config);

      IrcServerConnection isc = new IrcServerConnection(this.eventPublisher);
      isc.init(this, theBotConfig.getBotConfig().getBotName(), config);
//            addConnection(isc);

    } catch (Exception e) {
      log.error("RECONNECT FAILED", e);
    }
  }


  public void ircConnectionEstablished(IrcServerConnection connection) {
    log.debug("IRC connected: {}", connection);
    addConnection(connection);
  }

  public void ircConnectionEnded(IrcServerConnection connection) {
    log.debug("IRC connection ended: {}", connection.getId());
    IrcServerConnection remove = (IrcServerConnection) this.connectionMap.remove(connection.getId());
    removeJoinedChannelsForConnection(connection);
    log.debug("End IrcConnectionEnded: {}", remove);
    reconnectIrcServer(connection.getConfig());
  }


  public Map<Integer, BotConnection> getConnectionMap() {
    return this.connectionMap;
  }


  public void sendMessageByEchoToAlias(String messageText, String echoToAlias) throws InvalidEchoToAliasException {
    long startedAt = System.currentTimeMillis();
    log.debug(
        "ConnectionManager.sendMessageByEchoToAlias start echoToAlias={} messageLength={}",
        echoToAlias,
        messageText == null ? 0 : messageText.length()
    );
    Dual dual = findChannelByEchoToAlias(echoToAlias);
    if (dual != null) {
      BotConnectionChannel channel = dual.channel;
      BotConnection connection = dual.connection;

      Message message = Message.builder()
          .id(channel.getId())
          .message(messageText)
          .messageSource(MessageSource.NONE)
          .target(channel.getName())
          .build();

      log.debug(
          "ConnectionManager.sendMessageByEchoToAlias target resolved echoToAlias={} connectionId={} channel={} connectionType={}",
          echoToAlias,
          connection.getId(),
          channel.getName(),
          connection.getClass().getSimpleName()
      );
      connection.sendMessageTo(message);
      log.debug(
          "ConnectionManager.sendMessageByEchoToAlias sent echoToAlias={} durationMs={}",
          echoToAlias,
          System.currentTimeMillis() - startedAt
      );

    } else {
      log.warn(
          "ConnectionManager.sendMessageByEchoToAlias no channel echoToAlias={} durationMs={}",
          echoToAlias,
          System.currentTimeMillis() - startedAt
      );
      throw new InvalidEchoToAliasException("No channel found with echoToAlias: " + echoToAlias);
    }

  }

  public String sendIrcRawMessageByEchoToAlias(String rawCommand, String echoToAlias) throws InvalidEchoToAliasException, EchoToAliasNotIrcChannelException {
    Dual dual = findChannelByEchoToAlias(echoToAlias);
    if (dual != null) {
      BotConnectionChannel channel = dual.channel;
      BotConnection connection = dual.connection;

      if (!channel.getType().equals(BotConnectionType.IRC_CONNECTION.name())) {
        throw new EchoToAliasNotIrcChannelException("Target channel is not IRC channel type, can not send Raw Irc Message!");
      }


/*            Message message = Message.builder()
                    .id(channel.getId())
                    .message(messageText)
                    .messageSource(MessageSource.NONE)
                    .target(channel.getName())
                    .build();*/
      try {
        if (rawCommand.startsWith("WHOIS")) {
          IrcServerConnection ircConnection = (IrcServerConnection) connection;
          WhoisEvent whoisEvent = ircConnection.sendSyncWhois(rawCommand, 5000L);
          log.debug("Got WhoIs reply: {}", whoisEvent);
          return whoisEvent.toString();
        }

      } catch (InterruptedException e) {
        log.error("Sync operation failed", e);
      }


    } else {
      throw new InvalidEchoToAliasException("No channel found with echoToAlias: " + echoToAlias);
    }

    return null;
  }

  public List<ChannelUser> getChannelUsersByEchoToAlias(String echoToAlias) throws InvalidEchoToAliasException {
    Dual dual = findChannelByEchoToAlias(echoToAlias);
    if (dual == null) {
      throw new InvalidEchoToAliasException("No channel found with echoToAlias: " + echoToAlias);
    }
    List<ChannelUser> users = dual.connection.getChannelUsersByEchoToAlias(echoToAlias, dual.channel);

    return users;
  }

  private Dual findChannelByEchoToAlias(String echoToAlias) {
    JoinedChannelContainer container = this.joinedChannelsMap.get(normalizeEchoToAlias(echoToAlias));
    if (container != null) {
      Dual r = new Dual();
      r.connection = container.connection;
      r.channel = container.channel;
      return r;

    }
    return null;
  }

  private String normalizeEchoToAlias(String echoToAlias) {
    if (echoToAlias == null) {
      return null;
    }
    String trimmed = echoToAlias.trim();
    if (trimmed.isEmpty()) {
      return null;
    }
    return trimmed.toUpperCase();
  }

  private String normalizeLookup(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    if (trimmed.isEmpty()) {
      return null;
    }
    return trimmed.toLowerCase();
  }

  private String normalizeUserKey(
      BotConnectionType connectionType,
      String network,
      String userId,
      String username,
      String displayName) {
    String normalizedId = normalizeLookup(userId);
    String fallback = normalizeLookup(firstNonBlank(username, displayName));
    if (connectionType == null || (normalizedId == null && fallback == null)) {
      return null;
    }
    return connectionType.name() + "|"
        + (normalizeLookup(network) == null ? "unknown" : normalizeLookup(network)) + "|"
        + (normalizedId == null ? "name:" + fallback : "id:" + normalizedId);
  }

  private String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value;
      }
    }
    return null;
  }

  private BotConnectionChannel syntheticChannel(BotConnection connection, String echoToAlias) {
    BotConnectionChannel channel = new BotConnectionChannel();
    channel.setId(echoToAlias);
    channel.setEchoToAlias(echoToAlias);
    channel.setName(echoToAlias);
    channel.setNetwork(connection.getNetwork());
    channel.setType(connection.getType().name());
    return channel;
  }

  private List<User> readConfiguredUsers() {
    if (configuredUsersInjectedForTesting) {
      return configuredUsers;
    }
    if (configService == null) {
      return List.of();
    }
    try {
      File usersFile = configService.getRuntimeDataFile("users.json");
      if (!usersFile.exists()) {
        configuredUsers = List.of();
        configuredUsersLastModified = -1L;
        return configuredUsers;
      }
      long lastModified = usersFile.lastModified();
      if (lastModified == configuredUsersLastModified) {
        return configuredUsers;
      }
      JsonMapper mapper = objectMapper == null ? JsonMapper.builder().build() : objectMapper;
      UserValuesJsonContainer container = mapper.readValue(usersFile, UserValuesJsonContainer.class);
      List<User> users = container.getData_values() == null ? List.of() : new ArrayList<>(container.getData_values());
      configuredUsers = Collections.unmodifiableList(users);
      configuredUsersLastModified = lastModified;
      log.debug("Loaded configured users for target normalization: {}", configuredUsers.size());
    } catch (Exception e) {
      log.warn("Unable to load configured users for target normalization", e);
      configuredUsers = List.of();
      configuredUsersLastModified = -1L;
    }
    return configuredUsers;
  }

  private KnownUserTarget resolveKnownUserTarget(KnownUserPresence presence, List<User> users) {
    UserMatch match = findConfiguredUser(presence, users);
    User user = match == null ? null : match.user();
    String logicalUserKey = user == null ? presence.userKey : "configured:" + user.getId();
    return new KnownUserTarget(
        logicalUserKey,
        user == null ? null : user.getId(),
        user == null ? null : user.getUsername(),
        user == null ? null : user.getName(),
        user != null,
        match == null ? "OBSERVED_ONLY" : match.source(),
        presence);
  }

  private UserMatch findConfiguredUser(KnownUserPresence presence, List<User> users) {
    for (User user : users) {
      UserMatch match = matchConfiguredUser(presence, user);
      if (match != null) {
        return match;
      }
    }
    return null;
  }

  private UserMatch matchConfiguredUser(KnownUserPresence presence, User user) {
    BotConnectionType connectionType = parseConnectionType(presence.connectionType);
    if (connectionType == BotConnectionType.IRC_CONNECTION
        && configuredValueMatchesObserved(user.getIrcNick(), presence.userId, presence.username)) {
      return new UserMatch(user, "IRC_NICK");
    }
    if (connectionType == BotConnectionType.DISCORD_CONNECTION
        && configuredValueMatchesObserved(user.getDiscordId(), presence.userId)) {
      return new UserMatch(user, "DISCORD_ID");
    }
    if (connectionType == BotConnectionType.TELEGRAM_CONNECTION
        && configuredValueMatchesObserved(user.getTelegramId(), presence.userId)) {
      return new UserMatch(user, "TELEGRAM_ID");
    }
    if (configuredValueMatchesObserved(user.getUsername(), presence.username, presence.displayName)
        || configuredValueMatchesObserved(user.getName(), presence.username, presence.displayName)) {
      return new UserMatch(user, "NAME");
    }
    return null;
  }

  private BotConnectionType parseConnectionType(String connectionType) {
    if (connectionType == null) {
      return null;
    }
    try {
      return BotConnectionType.valueOf(connectionType);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  private boolean configuredValueMatchesObserved(String configuredValue, String... observedValues) {
    String normalizedConfigured = normalizeComparable(configuredValue);
    if (normalizedConfigured == null) {
      return false;
    }
    for (String observedValue : observedValues) {
      String normalizedObserved = normalizeComparable(observedValue);
      if (normalizedObserved != null && normalizedConfigured.equals(normalizedObserved)) {
        return true;
      }
    }
    return false;
  }

  private String normalizeComparable(String value) {
    String normalized = normalizeLookup(value);
    if (normalized == null || "none".equals(normalized) || "null".equals(normalized)) {
      return null;
    }
    return normalized;
  }

  private record LastChannelActivity(
      long timestamp,
      String actor,
      String source,
      String type,
      String network,
      String name) {
  }

  private static class KnownChannel {
    private int connectionId;
    private String connectionType;
    private String network;
    private String channelId;
    private String channelName;
    private String echoToAlias;
    private Long lastReceivedMessageAt;
    private String lastReceivedMessageBy;
    private String lastReceivedMessageSource;

    static KnownChannel from(BotConnectionType connectionType, BotConnection connection, BotConnectionChannel channel) {
      KnownChannel knownChannel = new KnownChannel();
      knownChannel.connectionId = connection.getId();
      knownChannel.connectionType = connectionType.name();
      knownChannel.network = channel.getNetwork();
      knownChannel.channelId = channel.getId();
      knownChannel.channelName = channel.getName();
      knownChannel.echoToAlias = channel.getEchoToAlias();
      return knownChannel;
    }

    KnownChatChannelResponse toResponse() {
      return new KnownChatChannelResponse(
          connectionId,
          connectionType,
          network,
          channelId,
          channelName,
          echoToAlias,
          lastReceivedMessageAt,
          lastReceivedMessageBy,
          lastReceivedMessageSource);
    }

    String sortKey() {
      return String.join("|", safe(connectionType), safe(network), safe(echoToAlias));
    }
  }

  private static class KnownUserPresence {
    private String userKey;
    private String userId;
    private String username;
    private String displayName;
    private int connectionId;
    private String connectionType;
    private String network;
    private String channelId;
    private String channelName;
    private String echoToAlias;
    private long lastSeenAt;
    private String lastSeenSource;

    static KnownUserPresence from(
        String userKey,
        String userId,
        String username,
        String displayName,
        BotConnection connection,
        BotConnectionChannel channel,
        long lastSeenAt,
        String lastSeenSource) {
      KnownUserPresence presence = new KnownUserPresence();
      presence.userKey = userKey;
      presence.userId = userId;
      presence.username = username;
      presence.displayName = displayName;
      presence.connectionId = connection.getId();
      presence.connectionType = connection.getType().name();
      presence.network = channel.getNetwork();
      presence.channelId = channel.getId();
      presence.channelName = channel.getName();
      presence.echoToAlias = channel.getEchoToAlias();
      presence.lastSeenAt = lastSeenAt;
      presence.lastSeenSource = lastSeenSource;
      return presence;
    }

    KnownChatUserResponse toResponse() {
      return new KnownChatUserResponse(
          userKey,
          userId,
          username,
          displayName,
          connectionId,
          connectionType,
          network,
          channelId,
          channelName,
          echoToAlias,
          lastSeenAt,
          lastSeenSource);
    }

    boolean matches(String normalizedQuery) {
      return contains(userKey, normalizedQuery)
          || contains(userId, normalizedQuery)
          || contains(username, normalizedQuery)
          || contains(displayName, normalizedQuery)
          || contains(echoToAlias, normalizedQuery)
          || contains(channelName, normalizedQuery);
    }

    String sortKey() {
      return String.join("|", safe(username), safe(displayName), safe(connectionType), safe(echoToAlias));
    }
  }

  private record UserMatch(User user, String source) {
  }

  private static class KnownUserTarget {
    private String logicalUserKey;
    private Long configuredUserId;
    private String configuredUsername;
    private String configuredName;
    private boolean matchedConfiguredUser;
    private String matchSource;
    private KnownUserPresence presence;

    KnownUserTarget(
        String logicalUserKey,
        Long configuredUserId,
        String configuredUsername,
        String configuredName,
        boolean matchedConfiguredUser,
        String matchSource,
        KnownUserPresence presence) {
      this.logicalUserKey = logicalUserKey;
      this.configuredUserId = configuredUserId;
      this.configuredUsername = configuredUsername;
      this.configuredName = configuredName;
      this.matchedConfiguredUser = matchedConfiguredUser;
      this.matchSource = matchSource;
      this.presence = presence;
    }

    KnownUserTargetResponse toResponse() {
      return new KnownUserTargetResponse(
          logicalUserKey,
          configuredUserId,
          configuredUsername,
          configuredName,
          matchedConfiguredUser,
          matchSource,
          presence.userKey,
          presence.userId,
          presence.username,
          presence.displayName,
          presence.connectionId,
          presence.connectionType,
          presence.network,
          presence.channelId,
          presence.channelName,
          presence.echoToAlias,
          "CHANNEL",
          presence.lastSeenAt,
          presence.lastSeenSource);
    }

    boolean matches(String normalizedQuery) {
      return contains(logicalUserKey, normalizedQuery)
          || contains(configuredUserId == null ? null : String.valueOf(configuredUserId), normalizedQuery)
          || contains(configuredUsername, normalizedQuery)
          || contains(configuredName, normalizedQuery)
          || presence.matches(normalizedQuery);
    }

    String sortKey() {
      return String.join(
          "|",
          safe(configuredUsername),
          safe(configuredName),
          safe(presence.username),
          safe(presence.connectionType),
          safe(presence.echoToAlias));
    }
  }

  private static boolean contains(String value, String query) {
    return value != null && value.toLowerCase().contains(query);
  }

  private static String safe(String value) {
    return value == null ? "" : value;
  }

  public void sendMessageToConnection(int connectionId, Message message) throws InvalidChannelIdException {

    BotConnection connection = this.connectionMap.get(connectionId);
    if (connection != null) {
      log.debug("sendTo: {}", connection);
      connection.sendMessageTo(message);
    } else {
      throw new InvalidChannelIdException("No connection found with connectionId: " + connectionId);
    }
  }

  public void sendRawMessageToConnection(int connectionId, Message message) throws InvalidChannelIdException {
    BotConnection connection = this.connectionMap.get(connectionId);
    if (connection != null) {
      connection.sendRawMessage(message);
    } else {
      throw new InvalidChannelIdException("No connection found with connectionId: " + connectionId);
    }
  }

  class Dual {
    public BotConnection connection;
    public BotConnectionChannel channel;
  }

}

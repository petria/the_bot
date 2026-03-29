package org.freakz.io.connections;


import org.freakz.common.exception.EchoToAliasNotIrcChannelException;
import org.freakz.common.exception.InvalidChannelIdException;
import org.freakz.common.exception.InvalidEchoToAliasException;
import org.freakz.common.model.botconfig.IrcServerConfig;
import org.freakz.common.model.botconfig.TheBotConfig;
import org.freakz.common.model.connectionmanager.ChannelUser;
import org.freakz.common.model.engine.status.ChannelMessageCounters;
import org.freakz.common.model.feed.Message;
import org.freakz.common.model.feed.MessageSource;
import org.freakz.io.config.ConfigService;
import org.freakz.io.contoller.SlackEventsController;
import org.kitteh.irc.client.library.event.user.WhoisEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ConnectionManager implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(ConnectionManager.class);
  private final Map<Integer, BotConnection> connectionMap = new HashMap<>();
  @Autowired
  private ConfigService configService;
  @Autowired
  private EventPublisher eventPublisher;
  @Autowired
  private SlackEventsController slackEventsController;
  private Map<String, JoinedChannelContainer> joinedChannelsMap = new HashMap<>();

  private Map<String, ChannelMessageCounters> countersMap = new HashMap<>();

  public Map<String, ChannelMessageCounters> getCountersMap() {
    return countersMap;
  }


  public void addMessageInOut(String connectionType, int in, int out) {
    ChannelMessageCounters counters = countersMap.computeIfAbsent(connectionType, k -> new ChannelMessageCounters());
    counters.in += in;
    counters.out += out;
  }

  public void updateJoinedChannelsMap(BotConnectionType botConnectionType, BotConnection connection, BotConnectionChannel channel) {
    JoinedChannelContainer container = joinedChannelsMap.get(channel.getEchoToAlias());
    if (container == null) {
      container = new JoinedChannelContainer();
      container.botConnectionType = botConnectionType;
      container.channel = channel;
      container.connection = connection;
    }
    if (channel.getEchoToAlias() == null) {
      int foo = 0;
    }
    joinedChannelsMap.put(channel.getEchoToAlias(), container);
  }

  public Map<String, JoinedChannelContainer> getJoinedChannelsMap() {
    return this.joinedChannelsMap;
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

    log.debug(">> Connecting SLACK");
    if (theBotConfig.getSlackConfig().isConnectStartup()) {
      SlackConnection slackConnection = new SlackConnection();
      slackConnection.init(this, theBotConfig.getBotConfig().getBotName(), theBotConfig.getSlackConfig(), slackEventsController, eventPublisher);
      addConnection(slackConnection);
    } else {
      log.warn("SLACK Startup connect disabled: {}", theBotConfig.getSlackConfig());
    }

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
    JoinedChannelContainer container = this.joinedChannelsMap.get(echoToAlias.toUpperCase());
    if (container != null) {
      Dual r = new Dual();
      r.connection = container.connection;
      r.channel = container.channel;
      return r;

    }
    return null;
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

package org.freakz.io.connections;

import org.freakz.common.model.botconfig.BotConfig;
import org.freakz.common.model.botconfig.IrcNetwork;
import org.freakz.common.model.botconfig.IrcServer;
import org.freakz.common.model.botconfig.IrcServerConfig;
import org.freakz.common.model.botconfig.TheBotConfig;
import org.freakz.common.model.feed.MessageSource;
import org.freakz.common.model.users.User;
import org.freakz.io.config.ConfigService;
import org.kitteh.irc.client.library.Client;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Verifies that every IRC client lifecycle releases the client library's
 * event bus threads.
 *
 * <p>Background: client-lib 9.x's {@code Client#shutdown()} never stops the
 * MBassador event bus, and the bus only stops itself when a connection-ended
 * event reports {@code canReconnect == false} — a final field fixed at event
 * creation. Dropped connections always report reconnectable, so each replaced
 * client leaked two {@code MsgDispatcher} threads. After enough reconnect
 * cycles (e.g. while an IRC server is unreachable) the JVM exhausts its
 * native thread budget (EAGAIN) and can no longer build new clients, which
 * is why the bot could not connect to a healthy replacement server.
 */
class IrcServerConnectionCloseTest {

  /** Nothing listens on 127.0.0.1:1, so connection attempts fail fast. */
  private static final IrcServerConfig DEAD_SERVER_CONFIG = IrcServerConfig.builder()
      .name("test-irc")
      .ircNetwork(new IrcNetwork("TestNet", new IrcServer("127.0.0.1", 1)))
      .channelList(List.of())
      .connectStartup(true)
      .build();

  private static final EventPublisher NOOP_EVENT_PUBLISHER = new EventPublisher() {
    @Override
    public void logMessage(MessageSource messageSource, String network, String channel, String sender, String message) {
    }

    @Override
    public User publishEvent(BotConnection connection, Object source, String echoToAlias) {
      return null;
    }
  };

  @Test
  void closeReleasesEventBusThreads() throws Exception {
    ConnectionManager connectionManager = newConnectionManager();
    try (HeldTcpServer server = new HeldTcpServer()) {
      Set<Long> baseline = msgDispatcherThreadIds();

      IrcServerConnection connection = new IrcServerConnection(NOOP_EVENT_PUBLISHER);
      connection.init(connectionManager, "TestBot", "Test Real Name", ircConfig(server.port()));
      Client client = connection.getClient();
      assertThat(client).isNotNull();

      awaitUntil(() -> msgDispatcherThreadIds().size() > baseline.size(), Duration.ofSeconds(15));
      Set<Long> clientThreads = new HashSet<>(msgDispatcherThreadIds());
      clientThreads.removeAll(baseline);
      assertThat(clientThreads).hasSizeGreaterThanOrEqualTo(2);

      connection.close();

      awaitUntil(() -> msgDispatcherThreadIds().stream().noneMatch(clientThreads::contains),
          Duration.ofSeconds(15));
      assertThat(connection.getClient()).isNull();

      // Idempotent: closing again is a no-op.
      connection.close();
      assertThat(connection.getClient()).isNull();
    }
  }

  @Test
  void stopReleasesEventBusThreads() throws Exception {
    ConnectionManager connectionManager = newConnectionManager();
    try (HeldTcpServer server = new HeldTcpServer()) {
      Set<Long> baseline = msgDispatcherThreadIds();

      IrcServerConnection connection = new IrcServerConnection(NOOP_EVENT_PUBLISHER);
      connection.init(connectionManager, "TestBot", "Test Real Name", ircConfig(server.port()));

      awaitUntil(() -> msgDispatcherThreadIds().size() > baseline.size(), Duration.ofSeconds(15));
      Set<Long> clientThreads = new HashSet<>(msgDispatcherThreadIds());
      clientThreads.removeAll(baseline);

      connection.stop();

      awaitUntil(() -> msgDispatcherThreadIds().stream().noneMatch(clientThreads::contains),
          Duration.ofSeconds(15));
      assertThat(connection.getClient()).isNull();
    }
  }

  @Test
  void reconnectCyclesKeepEventBusThreadsBounded() throws Exception {
    ConnectionManager connectionManager = newConnectionManager();
    connectionManager.setIrcReconnectWaitTimeForTesting(50L);
    Set<Long> baseline = msgDispatcherThreadIds();

    IrcServerConnection connection = new IrcServerConnection(NOOP_EVENT_PUBLISHER);
    connection.init(connectionManager, "TestBot", "Test Real Name", DEAD_SERVER_CONFIG);
    assertThat(connection.getClient()).isNotNull();

    // The connection to 127.0.0.1:1 is refused; the real connection-ended
    // event flows through handleConnectionEnded -> reconnectIrcServer, which
    // builds a replacement client. The original client must release its bus.
    awaitUntil(() -> connection.getClient() == null, Duration.ofSeconds(30));

    // Let several more replacement generations turn over...
    Thread.sleep(8000);

    Set<Long> live = new HashSet<>(msgDispatcherThreadIds());
    live.removeAll(baseline);
    // Without the fix each replaced client keeps two bus threads forever
    // (2 x generations). With the fix, at most one in-flight client's bus
    // (two threads) remains, plus a brief overlap while replacing it.
    assertThat(live).hasSizeLessThanOrEqualTo(4);
  }

  private static IrcServerConfig ircConfig(int port) {
    return IrcServerConfig.builder()
        .name("test-irc")
        .ircNetwork(new IrcNetwork("TestNet", new IrcServer("127.0.0.1", port)))
        .channelList(List.of())
        .connectStartup(true)
        .build();
  }

  private static ConnectionManager newConnectionManager() throws Exception {
    ConnectionManager connectionManager = new ConnectionManager();
    inject(connectionManager, "configService", new ConfigService() {
      @Override
      public TheBotConfig readBotConfig() {
        return TheBotConfig.builder()
            .botConfig(BotConfig.builder()
                .botName("TestBot")
                .ircRealName("Test Real Name")
                .build())
            .build();
      }
    });
    inject(connectionManager, "eventPublisher", NOOP_EVENT_PUBLISHER);
    return connectionManager;
  }

  private static void inject(Object target, String fieldName, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }

  private static Set<Long> msgDispatcherThreadIds() {
    return Thread.getAllStackTraces().keySet().stream()
        .filter(thread -> thread.getName().startsWith("MsgDispatcher-"))
        .map(Thread::threadId)
        .collect(Collectors.toUnmodifiableSet());
  }

  private static void awaitUntil(BooleanSupplier condition, Duration timeout) {
    long deadline = System.currentTimeMillis() + timeout.toMillis();
    while (System.currentTimeMillis() < deadline) {
      if (condition.getAsBoolean()) {
        return;
      }
      try {
        Thread.sleep(50);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        fail("Interrupted while waiting for condition");
      }
    }
    assertThat(condition.getAsBoolean())
        .as("condition did not become true within %s", timeout)
        .isTrue();
  }

  /**
   * Accepts TCP connections and holds them open without speaking IRC, so the
   * client never sees a connection-ended event (no background reconnect
   * chain while the close/stop tests run).
   */
  private static final class HeldTcpServer implements AutoCloseable {
    private final ServerSocket serverSocket;
    private final List<Socket> held = new ArrayList<>();

    HeldTcpServer() throws IOException {
      serverSocket = new ServerSocket(0);
      Thread acceptor = new Thread(() -> {
        while (!serverSocket.isClosed()) {
          try {
            held.add(serverSocket.accept());
          } catch (IOException e) {
            return;
          }
        }
      });
      acceptor.setDaemon(true);
      acceptor.start();
    }

    int port() {
      return serverSocket.getLocalPort();
    }

    @Override
    public void close() throws IOException {
      for (Socket socket : held) {
        try {
          socket.close();
        } catch (IOException ignored) {
          // best effort
        }
      }
      serverSocket.close();
    }
  }
}

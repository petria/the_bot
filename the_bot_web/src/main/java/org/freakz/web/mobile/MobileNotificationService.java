package org.freakz.web.mobile;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.freakz.common.model.mobile.MobileNotificationEvent;
import org.freakz.web.config.TheBotWebProperties;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Qualifier;
import tools.jackson.databind.json.JsonMapper;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class MobileNotificationService {
  private static final int MAX_PER_USER = 1000;
  private final JsonMapper mapper;
  private final Path file;
  private final boolean fcmEnabled;
  private final List<NotificationRecord> notifications;
  private final List<DeviceRecord> devices;
  private FirebaseMessaging firebaseMessaging;

  public MobileNotificationService(@Qualifier("jsonMapper") JsonMapper mapper, TheBotWebProperties properties) {
    this.mapper = mapper;
    this.file = Path.of(properties.getMobileNotificationsFile());
    State state = read();
    this.notifications = new ArrayList<>(state.notifications() == null ? List.of() : state.notifications());
    this.devices = new ArrayList<>(state.devices() == null ? List.of() : state.devices());
    this.fcmEnabled = properties.isMobileFcmEnabled();
    if (fcmEnabled) {
      this.firebaseMessaging = initializeFirebase(properties.getMobileFcmCredentialsFile());
    }
  }

  public synchronized void registerDevice(String username, String token, String deviceName, String platform) {
    if (blank(username) || blank(token)) throw new IllegalArgumentException("FCM token is required");
    Instant now = Instant.now();
    devices.removeIf(device -> token.equals(device.fcmToken()));
    devices.add(new DeviceRecord(UUID.randomUUID().toString(), username, token, deviceName, platform, now, now, false));
    save();
  }

  public synchronized void unregisterDevice(String username, String deviceId) {
    devices.removeIf(device -> username.equals(device.username()) && deviceId.equals(device.deviceId()));
    save();
  }

  public synchronized List<DeviceRecord> devices(String username) {
    return devices.stream().filter(device -> username.equals(device.username()) && !device.revoked()).toList();
  }

  public synchronized void accept(MobileNotificationEvent event) {
    if (event == null || blank(event.username()) || blank(event.body())) return;
    if (!blank(event.eventId()) && notifications.stream().anyMatch(item -> event.eventId().equals(item.eventId()))) return;
    NotificationRecord record = new NotificationRecord(
        event.eventId(), event.username(), event.type(), event.title(), event.body(),
        event.connectionType(), event.channelAlias(), event.command(),
        event.occurredAt() == null ? Instant.now() : event.occurredAt(), false);
    notifications.add(record);
    trimUser(event.username());
    save();
    sendFcm(record);
  }

  public synchronized List<NotificationRecord> list(String username) {
    return notifications.stream().filter(item -> username.equals(item.username()))
        .sorted(Comparator.comparing(NotificationRecord::occurredAt).reversed()).toList();
  }

  public synchronized void markRead(String username, String eventId) {
    replace(eventId, username, true);
    save();
  }

  public synchronized void markAllRead(String username) {
    for (int i = 0; i < notifications.size(); i++) {
      NotificationRecord item = notifications.get(i);
      if (username.equals(item.username())) notifications.set(i, item.withRead(true));
    }
    save();
  }

  private void replace(String eventId, String username, boolean read) {
    for (int i = 0; i < notifications.size(); i++) {
      NotificationRecord item = notifications.get(i);
      if (username.equals(item.username()) && eventId.equals(item.eventId())) {
        notifications.set(i, item.withRead(read));
        return;
      }
    }
  }

  private void trimUser(String username) {
    List<NotificationRecord> own = notifications.stream().filter(item -> username.equals(item.username()))
        .sorted(Comparator.comparing(NotificationRecord::occurredAt).reversed()).toList();
    if (own.size() <= MAX_PER_USER) return;
    notifications.removeAll(own.subList(MAX_PER_USER, own.size()));
  }

  private void sendFcm(NotificationRecord record) {
    if (!fcmEnabled || firebaseMessaging == null) return;
    for (DeviceRecord device : devices(record.username())) {
      try {
        Message message = Message.builder()
            .setToken(device.fcmToken())
            .setNotification(Notification.builder().setTitle(record.title()).setBody(record.body()).build())
            .putData("eventId", record.eventId())
            .putData("type", record.type())
            .build();
        firebaseMessaging.send(message);
      } catch (Exception e) {
        // A stale token must not prevent inbox persistence or another device delivery.
      }
    }
  }

  private FirebaseMessaging initializeFirebase(String credentialsFile) {
    if (blank(credentialsFile)) throw new IllegalStateException("Mobile FCM is enabled but credentials file is empty");
    try (FileInputStream input = new FileInputStream(credentialsFile)) {
      FirebaseOptions options = FirebaseOptions.builder().setCredentials(GoogleCredentials.fromStream(input)).build();
      FirebaseApp app = FirebaseApp.getApps().stream().findFirst().orElseGet(() -> FirebaseApp.initializeApp(options));
      return FirebaseMessaging.getInstance(app);
    } catch (IOException e) {
      throw new IllegalStateException("Could not initialize Firebase FCM", e);
    }
  }

  private State read() {
    if (!Files.isRegularFile(file)) return new State(List.of(), List.of());
    try {
      State state = mapper.readValue(file.toFile(), State.class);
      return state == null ? new State(List.of(), List.of()) : state;
    } catch (RuntimeException e) {
      return new State(List.of(), List.of());
    }
  }

  private void save() {
    try {
      Path parent = file.toAbsolutePath().getParent();
      if (parent != null) Files.createDirectories(parent);
      Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
      mapper.writeValue(temporary.toFile(), new State(notifications, devices));
      Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    } catch (IOException e) {
      throw new IllegalStateException("Could not save mobile notifications", e);
    }
  }

  private boolean blank(String value) { return value == null || value.isBlank(); }

  public record DeviceRecord(String deviceId, String username, String fcmToken, String deviceName,
                             String platform, Instant createdAt, Instant lastSeenAt, boolean revoked) {}
  public record NotificationRecord(String eventId, String username, String type, String title, String body,
                                   String connectionType, String channelAlias, String command,
                                   Instant occurredAt, boolean read) {
    NotificationRecord withRead(boolean value) {
      return new NotificationRecord(eventId, username, type, title, body, connectionType, channelAlias, command, occurredAt, value);
    }
  }
  private record State(List<NotificationRecord> notifications, List<DeviceRecord> devices) {}
}

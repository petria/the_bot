package org.freakz.hermesmanager.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.UUID;

import org.freakz.hermesmanager.config.HermesManagerProperties;
import org.springframework.stereotype.Service;

/** Signals the gateway's own supervisor through the shared profile directory. */
@Service
public class HermesGatewayService {

  private static final Duration TIMEOUT = Duration.ofSeconds(20);
  private static final String REQUEST = ".the-bot-gateway-request";
  private static final String RESULT = ".the-bot-gateway-result";

  private final HermesManagerProperties properties;

  public HermesGatewayService(HermesManagerProperties properties) {
    this.properties = properties;
  }

  public void restart(String profile) {
    request(profile, "restart");
  }

  public void stop(String profile) {
    request(profile, "stop");
  }

  public void start(String profile) {
    request(profile, "start");
  }

  private void request(String profile, String operation) {
    if (!profile.matches("[a-z0-9-]+") || !properties.ports().containsKey(profile)) {
      throw new IllegalArgumentException("Unknown Hermes gateway profile");
    }
    Path directory = properties.dataDir().resolve("profiles").resolve(profile);
    Path request = directory.resolve(REQUEST);
    String id = UUID.randomUUID().toString();
    try {
      Path temp = Files.createTempFile(directory, REQUEST, ".tmp");
      try {
        Files.writeString(temp, id + " " + operation + "\n");
        Files.move(temp, request, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      } finally {
        Files.deleteIfExists(temp);
      }
      long deadline = System.nanoTime() + TIMEOUT.toNanos();
      Path result = directory.resolve(RESULT);
      while (System.nanoTime() < deadline) {
        if (Files.isRegularFile(result) && Files.size(result) <= 64) {
          String value = Files.readString(result).strip();
          if (value.equals(id + " ok")) {
            return;
          }
          if (value.equals(id + " failed")) {
            throw new IllegalStateException("Hermes gateway supervisor rejected " + operation + " for " + profile);
          }
        }
        Thread.sleep(200);
      }
      throw new IllegalStateException("Hermes gateway supervisor timed out during " + operation + " for " + profile);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted waiting for Hermes gateway supervisor", e);
    } catch (IOException e) {
      throw new IllegalStateException("Could not communicate with Hermes gateway supervisor", e);
    } finally {
      try {
        if (Files.isRegularFile(request) && Files.size(request) <= 64 && Files.readString(request).startsWith(id + " ")) {
          Files.deleteIfExists(request);
        }
      } catch (IOException ignored) {
        // An outstanding request is identified by its operation ID.
      }
    }
  }
}

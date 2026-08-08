package org.freakz.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

/** Atomic mutations for runtime settings shared by the bot services. */
public final class RuntimeConfigStore {

  private static final Logger log = LoggerFactory.getLogger(RuntimeConfigStore.class);

  private RuntimeConfigStore() {
  }

  public static boolean updateIrcChannelTopic(
      Path configPath,
      String echoToAlias,
      String topic,
      JsonMapper mapper) throws IOException {
    if (configPath == null || echoToAlias == null || echoToAlias.isBlank()) {
      return false;
    }
    Path absolutePath = configPath.toAbsolutePath().normalize();
    Path parent = absolutePath.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }
    Path lockPath = absolutePath.resolveSibling(absolutePath.getFileName() + ".lock");
    try (FileChannel lockChannel = FileChannel.open(lockPath,
        StandardOpenOption.CREATE, StandardOpenOption.WRITE);
         FileLock ignored = lockChannel.lock()) {
      JsonNode parsed = mapper.readTree(Files.readString(absolutePath));
      if (!(parsed instanceof ObjectNode root)) {
        throw new IOException("Runtime config root must be a JSON object: " + absolutePath);
      }
      boolean updated = false;
      JsonNode servers = root.get("ircServerConfigs");
      if (servers instanceof ArrayNode serverArray) {
        for (JsonNode server : serverArray) {
          JsonNode channels = server == null ? null : server.get("channelList");
          if (!(channels instanceof ArrayNode channelArray)) {
            continue;
          }
          for (JsonNode channel : channelArray) {
            if (!(channel instanceof ObjectNode channelObject)) {
              continue;
            }
            JsonNode alias = channelObject.get("echoToAlias");
            if (alias != null && echoToAlias.equalsIgnoreCase(alias.asText())) {
              channelObject.put("topic", topic == null ? "" : topic);
              updated = true;
            }
          }
        }
      }
      if (!updated) {
        return false;
      }
      Path tempFile = Files.createTempFile(parent, absolutePath.getFileName().toString(), ".tmp");
      try {
        Files.writeString(tempFile,
            mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root),
            StandardCharsets.UTF_8);
        try {
          Files.move(tempFile, absolutePath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
          Files.move(tempFile, absolutePath, StandardCopyOption.REPLACE_EXISTING);
        }
      } finally {
        Files.deleteIfExists(tempFile);
      }
      log.debug("Updated IRC channel topic in {} for {}", absolutePath, echoToAlias);
      return true;
    }
  }
}

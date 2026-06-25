package org.freakz.engine.services.howto;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class HowtoIndexService {

  private static final Logger log = LoggerFactory.getLogger(HowtoIndexService.class);
  private static final Pattern TOKEN_SPLIT = Pattern.compile("[^\\p{L}\\p{N}._/-]+");
  private static final int TEXT_SNIPPET_LENGTH = 260;

  private final JsonMapper jsonMapper;
  private final Resource indexResource;
  private HowtoIndex index = HowtoIndex.empty();

  public HowtoIndexService(
      JsonMapper jsonMapper,
      @Value("classpath:howto/howto-index.json") Resource indexResource) {
    this.jsonMapper = jsonMapper;
    this.indexResource = indexResource;
  }

  @PostConstruct
  public void loadIndex() {
    try {
      if (!indexResource.exists()) {
        log.warn("Howto index resource not found: {}", indexResource);
        index = HowtoIndex.empty();
        return;
      }
      try (InputStream inputStream = indexResource.getInputStream()) {
        JsonNode root = jsonMapper.readTree(inputStream);
        List<HowtoChunk> chunks = new ArrayList<>();
        JsonNode chunksNode = root.get("chunks");
        if (chunksNode != null && chunksNode.isArray()) {
          for (JsonNode chunkNode : chunksNode) {
            chunks.add(new HowtoChunk(
                text(chunkNode, "id"),
                text(chunkNode, "title"),
                text(chunkNode, "area"),
                text(chunkNode, "sourcePath"),
                intValue(chunkNode, "chunkIndex"),
                stringList(chunkNode.get("keywords")),
                text(chunkNode, "text")
            ));
          }
        }
        index = new HowtoIndex(
            intValue(root, "schemaVersion"),
            text(root, "generatedAt"),
            text(root, "generator"),
            text(root, "sourceFilter"),
            intValue(root, "chunkCount"),
            stringList(root.get("allowlist")),
            List.copyOf(chunks));
        log.info("Loaded howto index: chunks={} generatedAt={}", chunks.size(), index.generatedAt());
      }
    } catch (Exception e) {
      log.warn("Failed to load howto index: {}", e.getMessage(), e);
      index = HowtoIndex.empty();
    }
  }

  public HowtoIndexSummary summary() {
    return new HowtoIndexSummary(
        isLoaded(),
        index.schemaVersion(),
        index.generatedAt(),
        index.generator(),
        index.sourceFilter(),
        index.chunkCount(),
        index.chunks().size(),
        index.allowlist());
  }

  public List<HowtoSearchResult> search(String query, int limit) {
    if (query == null || query.isBlank() || index.chunks().isEmpty()) {
      return List.of();
    }

    List<String> tokens = tokens(query);
    if (tokens.isEmpty()) {
      return List.of();
    }

    return index.chunks().stream()
        .map(chunk -> score(chunk, tokens))
        .filter(result -> result.score() > 0)
        .sorted(Comparator.comparingInt(HowtoSearchResult::score).reversed()
            .thenComparing(result -> result.chunk().title(), String.CASE_INSENSITIVE_ORDER)
            .thenComparing(result -> result.chunk().sourcePath(), String.CASE_INSENSITIVE_ORDER))
        .limit(Math.max(1, limit))
        .toList();
  }

  public boolean isLoaded() {
    return !index.chunks().isEmpty();
  }

  public String formatChatAnswer(String query, int limit) {
    if (!isLoaded()) {
      return "Howto index is not loaded in this bot-engine build.";
    }

    List<HowtoSearchResult> results = search(query, limit);
    if (results.isEmpty()) {
      return "I could not find matching web UI/config help. Try more specific words.";
    }

    StringBuilder output = new StringBuilder("Howto matches:");
    int index = 1;
    for (HowtoSearchResult result : results) {
      HowtoChunk chunk = result.chunk();
      output.append("\n")
          .append(index++)
          .append(". ")
          .append(chunk.title())
          .append(" [")
          .append(chunk.area())
          .append("] ")
          .append(snippet(chunk.text()));
    }
    return output.toString();
  }

  private HowtoSearchResult score(HowtoChunk chunk, List<String> tokens) {
    int score = 0;
    String title = lower(chunk.title());
    String area = lower(chunk.area());
    String sourcePath = lower(chunk.sourcePath());
    String text = lower(chunk.text());
    Set<String> keywords = new HashSet<>(chunk.keywords().stream().map(HowtoIndexService::lower).toList());

    for (String token : tokens) {
      if (title.contains(token)) {
        score += 20;
      }
      if (keywords.stream().anyMatch(keyword -> keyword.contains(token))) {
        score += 14;
      }
      if (area.contains(token)) {
        score += 8;
      }
      if (sourcePath.contains(token)) {
        score += 6;
      }
      if (text.contains(token)) {
        score += 2;
      }
    }

    return new HowtoSearchResult(score, chunk);
  }

  private static List<String> tokens(String query) {
    String[] parts = TOKEN_SPLIT.split(lower(query));
    List<String> tokens = new ArrayList<>();
    for (String part : parts) {
      if (part == null || part.isBlank() || part.length() < 2) {
        continue;
      }
      tokens.add(part);
    }
    return tokens;
  }

  private static String snippet(String text) {
    if (text == null || text.isBlank()) {
      return "";
    }
    String normalized = text.replaceAll("\\s+", " ").trim();
    if (normalized.length() <= TEXT_SNIPPET_LENGTH) {
      return normalized;
    }
    return normalized.substring(0, TEXT_SNIPPET_LENGTH - 3).trim() + "...";
  }

  private static String lower(String value) {
    return value == null ? "" : value.toLowerCase(Locale.ROOT);
  }

  private static String text(JsonNode node, String fieldName) {
    JsonNode value = node == null ? null : node.get(fieldName);
    return value == null || value.isNull() ? "" : value.asString();
  }

  private static int intValue(JsonNode node, String fieldName) {
    JsonNode value = node == null ? null : node.get(fieldName);
    return value == null || value.isNull() ? 0 : value.asInt();
  }

  private static List<String> stringList(JsonNode node) {
    if (node == null || !node.isArray()) {
      return List.of();
    }
    List<String> values = new ArrayList<>();
    for (JsonNode item : node) {
      if (item != null && !item.isNull()) {
        values.add(item.asString());
      }
    }
    return List.copyOf(values);
  }

  public record HowtoIndex(
      int schemaVersion,
      String generatedAt,
      String generator,
      String sourceFilter,
      int chunkCount,
      List<String> allowlist,
      List<HowtoChunk> chunks) {
    public static HowtoIndex empty() {
      return new HowtoIndex(0, "", "", "", 0, List.of(), List.of());
    }
  }

  public record HowtoIndexSummary(
      boolean loaded,
      int schemaVersion,
      String generatedAt,
      String generator,
      String sourceFilter,
      int declaredChunkCount,
      int loadedChunkCount,
      List<String> allowlist) {
  }

  public record HowtoChunk(
      String id,
      String title,
      String area,
      String sourcePath,
      int chunkIndex,
      List<String> keywords,
      String text) {
  }

  public record HowtoSearchResult(
      int score,
      HowtoChunk chunk) {
  }
}

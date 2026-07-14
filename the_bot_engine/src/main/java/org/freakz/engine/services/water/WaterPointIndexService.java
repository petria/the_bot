package org.freakz.engine.services.water;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Discovers Finnish water temperature stations from the SYKE HTML index. */
@Service
public class WaterPointIndexService {

  private static final Logger log = LoggerFactory.getLogger(WaterPointIndexService.class);

  private static final String ROOT_URL = "https://wwwi2.ymparisto.fi/i2/95/vesiA.html";
  private static final Pattern HTML_TEXT = Pattern.compile(">\\s*([^<>]+?)\\s*</a>", Pattern.CASE_INSENSITIVE);

  private final Duration indexCacheTtl;
  private final Duration refreshInterval;
  private final Duration requestTimeout;
  private final int regionParallelism;
  private final ExecutorService discoveryExecutor;
  private final Cache<String, Optional<String>> chartUrlCache;
  private volatile IndexSnapshot snapshot = new IndexSnapshot(List.of(), Instant.MIN);
  private CompletableFuture<List<WaterPoint>> refreshInFlight;

  public WaterPointIndexService() {
    this(Duration.ofHours(24), Duration.ofHours(6), Duration.ofSeconds(5), 8);
  }

  @Autowired
  public WaterPointIndexService(
      @Value("${the.bot.water.index-cache-ttl:24h}") Duration indexCacheTtl,
      @Value("${the.bot.water.index-refresh-interval:6h}") Duration refreshInterval,
      @Value("${the.bot.water.request-timeout:5s}") Duration requestTimeout,
      @Value("${the.bot.water.region-parallelism:8}") int regionParallelism) {
    this.indexCacheTtl = indexCacheTtl;
    this.refreshInterval = refreshInterval;
    this.requestTimeout = requestTimeout;
    this.regionParallelism = Math.max(1, regionParallelism);
    this.discoveryExecutor = Executors.newFixedThreadPool(this.regionParallelism, runnable -> {
      Thread thread = new Thread(runnable, "water-index-discovery");
      thread.setDaemon(true);
      return thread;
    });
    this.chartUrlCache = Caffeine.newBuilder()
        .maximumSize(4096)
        .expireAfterWrite(indexCacheTtl.compareTo(Duration.ofHours(6)) < 0
            ? indexCacheTtl : Duration.ofHours(6))
        .build();
  }

  public Resolution resolve(String requestedName) {
    String requested = requestedName == null ? "" : requestedName.trim();
    if (requested.isBlank()) {
      return Resolution.notFound(requested);
    }

    List<WaterPoint> discovered = getPoints();
    String key = normalize(requested);
    List<WaterPoint> exact = discovered.stream()
        .filter(point -> normalize(point.name()).equals(key))
        .toList();
    if (exact.size() == 1) {
      return Resolution.found(exact.getFirst());
    }
    if (exact.size() > 1) {
      return Resolution.ambiguous(exact);
    }

    List<WaterPoint> partial = discovered.stream()
        .filter(point -> normalize(point.name()).contains(key))
        .toList();
    return partial.size() == 1 ? Resolution.found(partial.getFirst()) :
        partial.isEmpty() ? Resolution.notFound(requested) : Resolution.ambiguous(partial);
  }

  @Scheduled(
      initialDelayString = "${the.bot.water.index-warmup-delay:30000}",
      fixedDelayString = "${the.bot.water.index-refresh-interval-ms:21600000}")
  public void scheduledRefresh() {
    refreshIndex();
  }

  private List<WaterPoint> getPoints() {
    IndexSnapshot current = snapshot;
    if (!current.points().isEmpty() && !isExpired(current)) {
      if (isDueForRefresh(current)) {
        refreshIndex();
      }
      return current.points();
    }

    CompletableFuture<List<WaterPoint>> refresh = refreshIndex();
    try {
      return refresh.get(Math.max(1, requestTimeout.plusSeconds(7).toSeconds()), TimeUnit.SECONDS);
    } catch (Exception error) {
      log.warn("Water station index is not ready: {}", error.getMessage());
      return current.points();
    }
  }

  private boolean isExpired(IndexSnapshot current) {
    return current.refreshedAt().plus(indexCacheTtl).isBefore(Instant.now());
  }

  private boolean isDueForRefresh(IndexSnapshot current) {
    return current.refreshedAt().plus(refreshInterval).isBefore(Instant.now());
  }

  private synchronized CompletableFuture<List<WaterPoint>> refreshIndex() {
    if (refreshInFlight != null) {
      return refreshInFlight;
    }
    CompletableFuture<List<WaterPoint>> future = CompletableFuture.supplyAsync(this::discover, discoveryExecutor);
    refreshInFlight = future;
    future.whenComplete((result, error) -> {
      synchronized (this) {
        if (refreshInFlight == future) {
          refreshInFlight = null;
        }
      }
      if (error == null && result != null && !result.isEmpty()) {
        snapshot = new IndexSnapshot(result, Instant.now());
        log.info("Water station index refreshed: points={}", result.size());
      } else if (error != null) {
        log.warn("Water station index refresh failed: {}", error.getMessage());
      }
    });
    return future;
  }

  private List<WaterPoint> discover() {
    long started = System.nanoTime();
    try {
      Document root = get(ROOT_URL);
      Map<String, String> regions = new LinkedHashMap<>();
      for (Element option : root.select("select[name=forma] option[value]")) {
        String value = option.attr("value").trim();
        if (!value.isBlank()) {
          regions.putIfAbsent(value, absolute(ROOT_URL, value));
        }
      }

      List<CompletableFuture<List<WaterPoint>>> regionLoads = regions.values().stream()
          .map(regionUrl -> CompletableFuture.supplyAsync(() -> discoverRegion(regionUrl), discoveryExecutor)
              .exceptionally(error -> {
                log.debug("Could not load water region {}: {}", regionUrl, error.getMessage());
                return List.of();
              }))
          .toList();
      CompletableFuture.allOf(regionLoads.toArray(CompletableFuture[]::new)).join();

      Map<String, WaterPoint> discovered = new LinkedHashMap<>();
      for (CompletableFuture<List<WaterPoint>> regionLoad : regionLoads) {
        for (WaterPoint point : regionLoad.join()) {
          discovered.putIfAbsent(normalize(point.name()), point);
        }
      }
      log.debug("Water index discovery completed in {} ms: regions={}, points={}",
          elapsedMillis(started), regions.size(), discovered.size());
      return discovered.values().stream()
          .sorted(Comparator.comparing(WaterPoint::name, String.CASE_INSENSITIVE_ORDER))
          .toList();
    } catch (Exception error) {
      log.warn("Water index discovery failed after {} ms: {}", elapsedMillis(started), error.getMessage());
      return List.of();
    }
  }

  private List<WaterPoint> discoverRegion(String regionUrl) {
    try {
      Document region = get(regionUrl);
      String regionBase = regionUrl.endsWith("/") ? regionUrl : regionUrl + "/";
      Map<String, WaterPoint> points = new LinkedHashMap<>();
      for (Element link : region.select("a[href]")) {
        String href = link.attr("href").trim();
        String name = stationName(link);
        if (!href.toLowerCase(Locale.ROOT).endsWith("wqfi.html") || name.isBlank()) {
          continue;
        }
        points.putIfAbsent(normalize(name), new WaterPoint(name, absolute(regionBase, href)));
      }
      return List.copyOf(points.values());
    } catch (Exception error) {
      log.debug("Could not load water region {}: {}", regionUrl, error.getMessage());
      return List.of();
    }
  }

  private String stationName(Element link) {
    String name = link.text().trim();
    if (!name.isBlank()) {
      return name;
    }
    Matcher matcher = HTML_TEXT.matcher(link.outerHtml());
    return matcher.find() ? matcher.group(1).trim() : "";
  }

  public String resolveTemperatureChartUrl(WaterPoint point) throws Exception {
    Optional<String> cached = chartUrlCache.getIfPresent(point.pageUrl());
    if (cached != null) {
      return cached.orElse(null);
    }

    long started = System.nanoTime();
    Document station = get(point.pageUrl());
    Element heading = station.select("b").stream()
        .filter(element -> normalize(element.text()).equals(normalize("Jokiveden lämpötila")))
        .findFirst().orElse(null);
    if (heading != null) {
      Element parent = heading.parent();
      Element image = parent == null ? null : parent.select("img[src$=twlyhyt.png]").first();
      if (image != null) {
        String chartUrl = absolute(point.pageUrl(), image.attr("src"));
        chartUrlCache.put(point.pageUrl(), Optional.of(chartUrl));
        log.debug("Water chart lookup completed in {} ms: point={}", elapsedMillis(started), point.name());
        return chartUrl;
      }
    }
    chartUrlCache.put(point.pageUrl(), Optional.empty());
    log.debug("Water chart lookup completed in {} ms: point={} chart=none", elapsedMillis(started), point.name());
    return null;
  }

  private Document get(String url) throws Exception {
      return Jsoup.connect(url)
          .userAgent("HokanTheBot water temperature command")
          .timeout((int) requestTimeout.toMillis())
          .sslContext(WaterSiteTls.context())
          .followRedirects(true)
          .get();
  }

  private long elapsedMillis(long started) {
    return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
  }

  @PreDestroy
  public void shutdown() {
    discoveryExecutor.shutdownNow();
  }

  private String absolute(String base, String relative) {
    return java.net.URI.create(base).resolve(relative).toString();
  }

  public static String normalize(String value) {
    String normalized = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .toLowerCase(Locale.ROOT)
        .replaceAll("[^\\p{Alnum}]+", " ")
        .trim();
    return normalized.replaceAll("\\s+", " ");
  }

  public record WaterPoint(String name, String pageUrl) {
  }

  private record IndexSnapshot(List<WaterPoint> points, Instant refreshedAt) {
  }

  public record Resolution(String requested, WaterPoint point, List<WaterPoint> matches) {
    static Resolution found(WaterPoint point) {
      return new Resolution(point.name(), point, List.of(point));
    }

    static Resolution notFound(String requested) {
      return new Resolution(requested, null, List.of());
    }

    static Resolution ambiguous(List<WaterPoint> matches) {
      return new Resolution("", null, List.copyOf(matches));
    }

    public boolean found() {
      return point != null;
    }

    public boolean ambiguous() {
      return point == null && matches.size() > 1;
    }

    public String displayName() {
      return point == null ? requested : point.name();
    }
  }
}

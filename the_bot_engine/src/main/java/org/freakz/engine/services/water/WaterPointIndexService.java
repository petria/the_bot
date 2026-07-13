package org.freakz.engine.services.water;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Discovers Finnish water temperature stations from the SYKE HTML index. */
@Service
public class WaterPointIndexService {

  private static final String ROOT_URL = "https://wwwi2.ymparisto.fi/i2/95/vesiA.html";
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);
  private static final Pattern HTML_TEXT = Pattern.compile(">\\s*([^<>]+?)\\s*</a>", Pattern.CASE_INSENSITIVE);

  private final AtomicReference<List<WaterPoint>> points = new AtomicReference<>();

  public Resolution resolve(String requestedName) {
    String requested = requestedName == null ? "" : requestedName.trim();
    if (requested.isBlank()) {
      return Resolution.notFound(requested);
    }

    List<WaterPoint> discovered = points.updateAndGet(existing -> existing == null ? discover() : existing);
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

  private List<WaterPoint> discover() {
    try {
      Document root = get(ROOT_URL);
      Map<String, String> regions = new LinkedHashMap<>();
      for (Element option : root.select("select[name=forma] option[value]")) {
        String value = option.attr("value").trim();
        if (!value.isBlank()) {
          regions.putIfAbsent(value, absolute(ROOT_URL, value));
        }
      }

      Map<String, WaterPoint> discovered = new LinkedHashMap<>();
      for (String regionUrl : regions.values()) {
        Document region = get(regionUrl);
        String regionBase = regionUrl.endsWith("/") ? regionUrl : regionUrl + "/";
        for (Element link : region.select("a[href]")) {
          String href = link.attr("href").trim();
          String name = stationName(link);
          if (!href.toLowerCase(Locale.ROOT).endsWith("wqfi.html") || name.isBlank()) {
            continue;
          }
          String pageUrl = absolute(regionBase, href);
          discovered.putIfAbsent(normalize(name), new WaterPoint(name, pageUrl));
        }
      }
      return discovered.values().stream()
          .sorted(Comparator.comparing(WaterPoint::name, String.CASE_INSENSITIVE_ORDER))
          .toList();
    } catch (Exception error) {
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
    Document station = get(point.pageUrl());
    Element heading = station.select("b").stream()
        .filter(element -> normalize(element.text()).equals(normalize("Jokiveden lämpötila")))
        .findFirst().orElse(null);
    if (heading != null) {
      Element parent = heading.parent();
      Element image = parent == null ? null : parent.select("img[src$=twlyhyt.png]").first();
      if (image != null) {
        return absolute(point.pageUrl(), image.attr("src"));
      }
    }
    return null;
  }

  private Document get(String url) throws Exception {
      return Jsoup.connect(url)
          .userAgent("HokanTheBot water temperature command")
          .timeout((int) REQUEST_TIMEOUT.toMillis())
          .sslContext(WaterSiteTls.context())
          .followRedirects(true)
          .get();
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

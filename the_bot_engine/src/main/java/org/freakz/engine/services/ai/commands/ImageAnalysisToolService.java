package org.freakz.engine.services.ai.commands;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.freakz.engine.services.urls.UrlSecurityValidator;
import org.freakz.engine.services.water.WaterSiteTls;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;

@Service
public class ImageAnalysisToolService {

  private static final long MAX_IMAGE_BYTES = 5L * 1024L * 1024L;
  private static final int MAX_REDIRECTS = 3;

  private final UrlSecurityValidator securityValidator;
  private final JsonMapper jsonMapper;
  private final HttpClient httpClient;
  private final Cache<String, ImageData> cache = Caffeine.newBuilder()
      .maximumSize(32)
      .expireAfterWrite(Duration.ofMinutes(10))
      .build();

  public ImageAnalysisToolService(UrlSecurityValidator securityValidator, JsonMapper jsonMapper) {
    this.securityValidator = securityValidator;
    this.jsonMapper = jsonMapper;
    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .followRedirects(HttpClient.Redirect.NEVER)
        .build();
  }

  public String analyze(JsonNodeArguments arguments) {
    String sourceUrl = arguments.text("url", "imageUrl", "image_url");
    if (sourceUrl.isBlank()) {
      return error("image.analyze requires an image URL");
    }
    try {
      URI requested = URI.create(sourceUrl.trim());
      ImageData image = loadImage(sourceUrl.trim());
      ObjectNode result = jsonMapper.createObjectNode();
      result.put("tool", "image.analyze");
      result.put("sourceUrl", sourceUrl.trim());
      result.put("mediaType", image.mediaType());
      result.put("imageDataUrl", image.dataUrl());
      String question = arguments.text("question", "prompt");
      if (!question.isBlank()) {
        result.put("question", question);
      }
      return result.toString();
    } catch (Exception e) {
      return error("Could not load image: " + safeMessage(e));
    }
  }

  public ImageData loadImage(String sourceUrl) throws Exception {
    String normalizedUrl = sourceUrl == null ? "" : sourceUrl.trim();
    if (normalizedUrl.isBlank()) {
      throw new IllegalArgumentException("image URL is blank");
    }
    ImageData image = cache.getIfPresent(normalizedUrl);
    if (image != null) {
      return image;
    }
    image = download(URI.create(normalizedUrl));
    cache.put(normalizedUrl, image);
    return image;
  }

  public ImageData loadWaterSiteImage(String sourceUrl) throws Exception {
    String normalizedUrl = sourceUrl == null ? "" : sourceUrl.trim();
    if (!normalizedUrl.startsWith("https://wwwi2.ymparisto.fi/")) {
      throw new IllegalArgumentException("water image URL is not allowed");
    }
    URI requested = URI.create(normalizedUrl);
    if (!securityValidator.isAllowed(requested)) {
      throw new IllegalArgumentException("water image URL is not allowed");
    }
    return download(requested, WaterSiteTls.context());
  }

  private ImageData download(URI initialUri) throws Exception {
    return download(initialUri, null);
  }

  private ImageData download(URI initialUri, javax.net.ssl.SSLContext sslContext) throws Exception {
    URI current = initialUri;
    for (int redirect = 0; redirect <= MAX_REDIRECTS; redirect++) {
      if (!securityValidator.isAllowed(current)) {
        throw new IllegalArgumentException("image URL is not allowed");
      }
      HttpRequest request = HttpRequest.newBuilder(current)
          .timeout(Duration.ofSeconds(15))
          .header("User-Agent", "HokanTheBot image analyzer")
          .header("Accept", "image/*")
          .GET()
          .build();
      HttpClient client = sslContext == null ? httpClient : HttpClient.newBuilder()
          .connectTimeout(Duration.ofSeconds(5))
          .followRedirects(HttpClient.Redirect.NEVER)
          .sslContext(sslContext)
          .build();
      HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
      int status = response.statusCode();
      if (status >= 300 && status < 400) {
        String location = response.headers().firstValue("location").orElse(null);
        if (location == null || redirect == MAX_REDIRECTS) {
          throw new IllegalArgumentException("too many image redirects");
        }
        current = current.resolve(location);
        if (sslContext != null && !"wwwi2.ymparisto.fi".equalsIgnoreCase(current.getHost())) {
          throw new IllegalArgumentException("water image redirect is not allowed");
        }
        continue;
      }
      if (status < 200 || status >= 300) {
        throw new IllegalArgumentException("HTTP " + status);
      }
      byte[] bytes = response.body();
      if (bytes.length == 0 || bytes.length > MAX_IMAGE_BYTES) {
        throw new IllegalArgumentException("image exceeds the 5 MB limit");
      }
      String mediaType = normalizeImageType(response.headers().firstValue("content-type").orElse(null));
      BufferedImage decoded = mediaType == null ? null : ImageIO.read(new ByteArrayInputStream(bytes));
      if (decoded == null || decoded.getWidth() > 10_000 || decoded.getHeight() > 10_000
          || (long) decoded.getWidth() * decoded.getHeight() > 40_000_000L) {
        throw new IllegalArgumentException("response is not a supported image");
      }
      return new ImageData(mediaType, "data:" + mediaType + ";base64," + Base64.getEncoder().encodeToString(bytes));
    }
    throw new IllegalArgumentException("could not load image");
  }

  private String normalizeImageType(String contentType) {
    String value = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
    int separator = value.indexOf(';');
    if (separator >= 0) {
      value = value.substring(0, separator);
    }
    return switch (value.trim()) {
      case "image/png", "image/jpeg", "image/gif" -> value.trim();
      default -> null;
    };
  }

  private String error(String message) {
    ObjectNode result = jsonMapper.createObjectNode();
    result.put("tool", "image.analyze");
    result.put("error", message);
    return result.toString();
  }

  private String safeMessage(Exception error) {
    String message = error.getMessage();
    return message == null || message.isBlank() ? error.getClass().getSimpleName() : message;
  }

  public record JsonNodeArguments(tools.jackson.databind.JsonNode node) {
    String text(String... names) {
      for (String name : names) {
        String value = node == null ? "" : node.path(name).asString("").trim();
        if (!value.isBlank()) {
          return value;
        }
      }
      return "";
    }
  }

  public record ImageData(String mediaType, String dataUrl) {
  }
}

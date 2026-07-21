package org.freakz.engine.services.urls.resolver;

import org.freakz.engine.services.urls.UrlResolution;
import org.freakz.engine.services.urls.UrlResolverProperties;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Component
@Order(50)
public class NettiautoUrlResolver implements UrlResolver {

  private static final Pattern LISTING_PATH = Pattern.compile("^/[^/]+/[^/]+/[0-9]+/?$");
  private final SafeUrlDocumentFetcher documentFetcher;
  private final UrlResolverProperties properties;
  private final JsonMapper jsonMapper;

  public NettiautoUrlResolver(
      SafeUrlDocumentFetcher documentFetcher,
      UrlResolverProperties properties,
      JsonMapper jsonMapper) {
    this.documentFetcher = documentFetcher;
    this.properties = properties;
    this.jsonMapper = jsonMapper;
  }

  @Override
  public boolean supports(URI uri) {
    if (!properties.getNettiauto().isEnabled() || uri == null || uri.getHost() == null) {
      return false;
    }
    String host = uri.getHost().toLowerCase(Locale.ROOT);
    return (host.equals("nettiauto.com") || host.equals("www.nettiauto.com"))
        && uri.getPath() != null
        && LISTING_PATH.matcher(uri.getPath()).matches();
  }

  @Override
  public Optional<UrlResolution> resolve(URI uri) {
    if (!supports(uri)) {
      return Optional.empty();
    }
    return documentFetcher.fetch(uri).flatMap(document -> resolve(uri, document));
  }

  Optional<UrlResolution> resolve(URI uri, Document document) {
    JsonNode productInfo = findProductInfo(document).orElse(null);
    JsonNode structuredData = findStructuredData(document).orElse(null);

    String pageTitle = firstMeta(document, "meta[property=og:title]", "meta[name=twitter:title]");
    if (pageTitle == null) {
      pageTitle = clean(document.title());
    }
    if (pageTitle == null) {
      return Optional.empty();
    }

    String registration = firstText(
        text(productInfo, "registrationNumber"),
        text(structuredData, "vehicleIdentificationNumber"),
        textByLabel(document, "Rekisterinumero"));
    String mileage = firstText(
        text(productInfo, "mileageFromOdometer"),
        text(structuredData == null ? null : structuredData.path("mileageFromOdometer"), "value"),
        textByLabel(document, "Mittarilukema"));
    String price = firstText(
        text(productInfo, "basePrice"),
        text(structuredData == null ? null : structuredData.path("offers"), "price"));
    String power = textByLabel(document, "Teho");

    Map<String, String> attributes = new LinkedHashMap<>();
    put(attributes, "vehicleName", titlePart(pageTitle, 0));
    put(attributes, "listingTitle", titlePart(pageTitle, 1));
    put(attributes, "registrationNumber", clean(registration));
    put(attributes, "price", normalizeNumber(price));
    put(attributes, "power", normalizePower(power));
    put(attributes, "mileage", normalizeNumber(mileage));

    boolean hasVehicleData = attributes.keySet().stream()
        .filter(key -> !key.equals("vehicleName") && !key.equals("listingTitle"))
        .map(attributes::get)
        .anyMatch(value -> value != null);
    if (!hasVehicleData) {
      return Optional.empty();
    }

    return Optional.of(new UrlResolution(
        uri,
        "Nettiauto",
        pageTitle,
        null,
        null,
        null,
        null,
        null,
        attributes));
  }

  private Optional<JsonNode> findProductInfo(Document document) {
    return findJsonObject(document, "productInfo");
  }

  private Optional<JsonNode> findStructuredData(Document document) {
    for (Element script : document.select("script[type=application/ld+json]")) {
      try {
        JsonNode node = jsonMapper.readTree(script.data());
        if (node != null) {
          return Optional.of(node);
        }
      } catch (Exception ignored) {
        // Continue with the other structured-data blocks.
      }
    }
    return Optional.empty();
  }

  private Optional<JsonNode> findJsonObject(Document document, String field) {
    for (Element script : document.select("script")) {
      String data = script.data();
      int start = data.indexOf("{\"" + field + "\"");
      if (start < 0) {
        continue;
      }
      try {
        JsonNode root = jsonMapper.readTree(data.substring(start));
        return Optional.ofNullable(root == null ? null : root.path(field));
      } catch (Exception ignored) {
        // Continue with the other scripts.
      }
    }
    return Optional.empty();
  }

  private String text(JsonNode node, String field) {
    if (node == null || node.isMissingNode() || node.isNull()) {
      return null;
    }
    JsonNode value = node.path(field);
    return value.isValueNode() ? clean(value.asString(null)) : null;
  }

  private String textByLabel(Document document, String label) {
    for (Element box : document.select(".vehicle-info-box")) {
      Element name = box.selectFirst(".vehicle-info-box__vehicle-info");
      if (name != null && label.equalsIgnoreCase(clean(name.text()))) {
        Element value = box.selectFirst(".vehicle-info-box__vehicle-det");
        return value == null ? null : clean(value.text());
      }
    }
    return null;
  }

  private String firstMeta(Document document, String... selectors) {
    for (String selector : selectors) {
      Element element = document.selectFirst(selector);
      if (element != null) {
        String value = clean(element.attr("content"));
        if (value != null) {
          return value;
        }
      }
    }
    return null;
  }

  private String titlePart(String title, int index) {
    String[] parts = title.split("/", 2);
    if (index == 0) {
      return clean(parts[0]);
    }
    return parts.length == 2 ? clean(parts[1]) : null;
  }

  private String normalizeNumber(String value) {
    if (value == null) {
      return null;
    }
    String normalized = value.replace(" ", "").replace(',', '.').trim();
    try {
      return new BigDecimal(normalized).stripTrailingZeros().toPlainString();
    } catch (NumberFormatException ignored) {
      return clean(value);
    }
  }

  private String normalizePower(String value) {
    return value == null ? null : value.replaceAll("\\s+", "").trim();
  }

  private String firstText(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value;
      }
    }
    return null;
  }

  private void put(Map<String, String> attributes, String key, String value) {
    if (value != null && !value.isBlank()) {
      attributes.put(key, value);
    }
  }

  private String clean(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.replaceAll("\\s+", " ").trim();
  }
}

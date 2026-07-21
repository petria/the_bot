package org.freakz.engine.services.urls;

import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class UrlResolutionFormatter {

  private static final String BOLD = "\u0002";
  private static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC);

  public String format(UrlResolution resolution) {
    if ("Nettiauto".equalsIgnoreCase(resolution.provider())) {
      return formatNettiauto(resolution);
    }
    String title = clean(resolution.title());
    if (title == null) {
      return null;
    }

    String provider = clean(resolution.provider());
    if (provider == null || "Web".equalsIgnoreCase(provider)) {
      return "[ " + BOLD + title + BOLD + " ]";
    }

    StringBuilder result = new StringBuilder()
        .append(BOLD).append('[').append(provider).append(']').append(BOLD)
        .append(' ').append(title);
    if ("Wikipedia".equalsIgnoreCase(provider) && clean(resolution.description()) != null) {
      result.append(" - ").append(abbreviate(clean(resolution.description()), 350));
    }
    if (clean(resolution.author()) != null) {
      result.append(" by ").append(clean(resolution.author()));
    }
    if (resolution.duration() != null) {
      result.append(" (").append(formatDuration(resolution.duration().toSeconds())).append(')');
    }
    if (resolution.publishedAt() != null) {
      result.append(" [").append(DATE_FORMATTER.format(resolution.publishedAt())).append(']');
    }
    if (resolution.viewCount() != null) {
      result.append(" [").append(resolution.viewCount()).append(" views]");
    }
    return result.toString();
  }

  private String formatNettiauto(UrlResolution resolution) {
    var attributes = resolution.attributes();
    String vehicleName = value(attributes, "vehicleName", resolution.title());
    String registration = value(attributes, "registrationNumber", "N/A");
    String price = value(attributes, "price", "N/A");
    String power = value(attributes, "power", "N/A");
    String mileage = value(attributes, "mileage", "N/A");
    String listingTitle = value(attributes, "listingTitle", "N/A");
    if (!"N/A".equals(price)) {
      price += "€";
    }
    if (!"N/A".equals(mileage)) {
      mileage += "km";
    }
    return "[ " + BOLD + vehicleName + " / " + registration + " / " + price + " / "
        + power + " / " + mileage + " / " + listingTitle + BOLD + " ]";
  }

  private String value(java.util.Map<String, String> attributes, String key, String fallback) {
    String value = attributes == null ? null : clean(attributes.get(key));
    return value == null ? fallback : value;
  }

  private String formatDuration(long seconds) {
    long hours = seconds / 3600;
    long minutes = (seconds % 3600) / 60;
    long remainingSeconds = seconds % 60;
    if (hours > 0) {
      return "%d:%02d:%02d".formatted(hours, minutes, remainingSeconds);
    }
    return "%d:%02d".formatted(minutes, remainingSeconds);
  }

  private String clean(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.replaceAll("\\s+", " ").trim();
  }

  private String abbreviate(String value, int maximumLength) {
    if (value.length() <= maximumLength) {
      return value;
    }
    return value.substring(0, maximumLength - 3).trim() + "...";
  }
}

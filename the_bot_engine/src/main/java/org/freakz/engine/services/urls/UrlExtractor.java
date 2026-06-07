package org.freakz.engine.services.urls;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class UrlExtractor {

  private static final Pattern URL_PATTERN =
      Pattern.compile("(?i)(?:https?://|www\\.)[^\\s<>]+");
  private static final String TRAILING_PUNCTUATION = ".,!?;:'\"";

  public List<URI> extract(String message, int limit) {
    if (message == null || message.isBlank() || limit <= 0) {
      return List.of();
    }

    Set<URI> urls = new LinkedHashSet<>();
    Matcher matcher = URL_PATTERN.matcher(message);
    while (matcher.find() && urls.size() < limit) {
      URI normalized = normalize(matcher.group());
      if (normalized != null) {
        urls.add(normalized);
      }
    }
    return new ArrayList<>(urls);
  }

  private URI normalize(String candidate) {
    String value = candidate;
    while (!value.isEmpty() && isTrailingPunctuation(value.charAt(value.length() - 1))) {
      value = value.substring(0, value.length() - 1);
    }
    value = removeUnmatchedClosingCharacters(value);
    if (value.regionMatches(true, 0, "www.", 0, 4)) {
      value = "https://" + value;
    }

    try {
      URI uri = new URI(value).normalize();
      if (uri.getScheme() == null || uri.getHost() == null) {
        return null;
      }
      return new URI(
          uri.getScheme().toLowerCase(),
          uri.getUserInfo(),
          uri.getHost().toLowerCase(),
          uri.getPort(),
          uri.getPath(),
          uri.getQuery(),
          null);
    } catch (URISyntaxException ex) {
      return null;
    }
  }

  private boolean isTrailingPunctuation(char value) {
    return TRAILING_PUNCTUATION.indexOf(value) >= 0;
  }

  private String removeUnmatchedClosingCharacters(String value) {
    String result = value;
    result = removeUnmatchedClosingCharacter(result, '(', ')');
    result = removeUnmatchedClosingCharacter(result, '[', ']');
    result = removeUnmatchedClosingCharacter(result, '{', '}');
    return result;
  }

  private String removeUnmatchedClosingCharacter(String value, char opening, char closing) {
    while (value.endsWith(String.valueOf(closing))
        && count(value, closing) > count(value, opening)) {
      value = value.substring(0, value.length() - 1);
    }
    return value;
  }

  private long count(String value, char character) {
    return value.chars().filter(current -> current == character).count();
  }
}

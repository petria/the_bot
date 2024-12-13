package org.freakz.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class FeignUtils {

  public static <T> Optional<T> getResponseBody(
      Response response, Class<T> klass, ObjectMapper mapper) {
    try {
      String bodyJson =
          new BufferedReader(new InputStreamReader(response.body().asInputStream()))
              .lines()
              .parallel()
              .collect(Collectors.joining("\n"));
      return Optional.ofNullable(mapper.readValue(bodyJson, klass));
    } catch (IOException e) {
      log.error("Error when read feign response.", e);
      return Optional.empty();
    }
  }
}

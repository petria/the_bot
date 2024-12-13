package org.freakz.engine.services.weather.weatherapi;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class CustomLocalDateTimeDeserializer extends StdDeserializer<LocalDateTime> {

  public CustomLocalDateTimeDeserializer() {
    super((Class<?>) null);
  }

  @Override
  public LocalDateTime deserialize(
      JsonParser jsonParser, DeserializationContext deserializationContext)
      throws IOException, JacksonException {
    String dateStr = jsonParser.getText();
    LocalDateTime time;
    try {
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
      time = LocalDateTime.parse(dateStr, formatter);

    } catch (DateTimeParseException e) {
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm");
      time = LocalDateTime.parse(dateStr, formatter);
    }
    return time;
  }
}

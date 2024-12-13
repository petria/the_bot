package org.freakz.engine.functions;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.function.Function;

/*
  Weather API
  https://www.weatherapi.com/api-explorer.aspx
*/
@Slf4j
public class TimeService implements Function<TimeService.Request, TimeService.Response> {

  public TimeService() {
    log.debug("Init!");
  }

  @Override
  public Response apply(Request timeRequest) {
    log.info("Time Request: {}", timeRequest);
    Response response = new Response(new Time(LocalDateTime.now().toString()));
    log.info("Time API Response: {}", response);
    return response;
  }

  // mapping the response of the Weather API to records. I only mapped the information I was
  // interested in.
  public record Request(String city) {}

  public record Response(Time time) {}

  public record Time(String timeNow) {}
}

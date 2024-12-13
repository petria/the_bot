package org.freakz.engine.functions;

import lombok.extern.slf4j.Slf4j;

import java.util.function.Function;

@Slf4j
public class MyLocationService
    implements Function<MyLocationService.Request, MyLocationService.Response> {

  public MyLocationService() {
    log.debug("Init!");
  }

  @Override
  public Response apply(Request locationRequest) {
    log.info("Location Request: {}", locationRequest);
    Response response = new Response(new MyLocation("Hetzner datacenter"));
    log.info("Location API Response: {}", response);
    return response;
  }

  // mapping the response of the Weather API to records. I only mapped the information I was
  // interested in.
  public record Request(String context) {}

  public record Response(MyLocation location) {}

  public record MyLocation(String place) {}
}

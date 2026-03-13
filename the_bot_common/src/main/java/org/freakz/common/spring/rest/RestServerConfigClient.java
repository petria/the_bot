package org.freakz.common.spring.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class RestServerConfigClient {

  private static final Logger log = LoggerFactory.getLogger(RestServerConfigClient.class);
  private final RestTemplate restTemplate;
  private final String BASE_URL = "http://bot-io:8090/api/hokan/io/server_config";

  @Autowired
  public RestServerConfigClient(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  public String reloadConfig() {
    String url = BASE_URL + "/reload";
    try {
      return restTemplate.getForObject(url, String.class);
    } catch (Exception e) {
      log.error("Error sending message by target alias: {}", e.getMessage());
      return e.getMessage();
    }
  }

}

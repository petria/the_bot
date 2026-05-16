package org.freakz.web.controller;

import org.freakz.common.config.TheBotProperties;
import org.freakz.common.generated.GeneratedPagePublicResponse;
import org.freakz.common.generated.GeneratedPageStore;
import org.freakz.web.config.TheBotWebProperties;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Path;

@RestController
@RequestMapping("/api/web/generated-pages")
public class GeneratedPagesController {

  private final TheBotProperties properties;
  private final TheBotWebProperties webProperties;
  private final JsonMapper jsonMapper;

  public GeneratedPagesController(
      TheBotProperties properties,
      TheBotWebProperties webProperties,
      JsonMapper jsonMapper) {
    this.properties = properties;
    this.webProperties = webProperties;
    this.jsonMapper = jsonMapper;
  }

  @GetMapping("/{id}")
  public GeneratedPagePublicResponse getGeneratedPage(
      @PathVariable String id,
      @RequestParam String token) {
    try {
      GeneratedPageStore store = new GeneratedPageStore(resolveDataDir(), jsonMapper);
      return store.readPublic(id, token)
          .map(GeneratedPagePublicResponse::fromRecord)
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    } catch (ResponseStatusException e) {
      throw e;
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
  }

  private Path resolveDataDir() {
    Path usersFile = Path.of(webProperties.getUsersFile());
    Path parent = usersFile.getParent();
    if (parent != null) {
      return parent;
    }
    return Path.of(properties.getDataDir());
  }
}

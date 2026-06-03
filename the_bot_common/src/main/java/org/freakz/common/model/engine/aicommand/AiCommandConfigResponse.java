package org.freakz.common.model.engine.aicommand;

import java.util.ArrayList;
import java.util.List;

public class AiCommandConfigResponse {

  private String path;
  private AiCommandConfig config = new AiCommandConfig();
  private List<String> availableTools = new ArrayList<>();

  public AiCommandConfigResponse() {
  }

  public AiCommandConfigResponse(String path, AiCommandConfig config, List<String> availableTools) {
    this.path = path;
    this.config = config == null ? new AiCommandConfig() : config;
    this.availableTools = availableTools == null ? new ArrayList<>() : new ArrayList<>(availableTools);
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public AiCommandConfig getConfig() {
    return config;
  }

  public void setConfig(AiCommandConfig config) {
    this.config = config == null ? new AiCommandConfig() : config;
  }

  public List<String> getAvailableTools() {
    return availableTools;
  }

  public void setAvailableTools(List<String> availableTools) {
    this.availableTools = availableTools == null ? new ArrayList<>() : new ArrayList<>(availableTools);
  }
}

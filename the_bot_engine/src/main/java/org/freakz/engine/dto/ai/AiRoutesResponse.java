package org.freakz.engine.dto.ai;

import org.freakz.engine.services.api.ServiceResponse;

import java.util.List;

public class AiRoutesResponse extends ServiceResponse {

  private List<String> lines;

  public AiRoutesResponse() {
  }

  public AiRoutesResponse(List<String> lines) {
    this.lines = lines;
  }

  public List<String> getLines() {
    return lines;
  }

  public void setLines(List<String> lines) {
    this.lines = lines;
  }
}

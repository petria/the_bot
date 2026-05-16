package org.freakz.engine.dto.generated;

import org.freakz.engine.services.api.ServiceResponse;

public class GeneratedPageResponse extends ServiceResponse {

  private String url;
  private int rowCount;

  public GeneratedPageResponse() {
  }

  public GeneratedPageResponse(String url, int rowCount) {
    this.url = url;
    this.rowCount = rowCount;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public int getRowCount() {
    return rowCount;
  }

  public void setRowCount(int rowCount) {
    this.rowCount = rowCount;
  }
}

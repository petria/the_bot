package org.freakz.common.model.connectionmanager;

public class SendMessageToKnownUserRequest {

  private String query;
  private String message;
  private Boolean preferPrivate;
  private String connectionType;
  private String echoToAlias;

  public SendMessageToKnownUserRequest() {
  }

  public SendMessageToKnownUserRequest(
      String query,
      String message,
      Boolean preferPrivate,
      String connectionType,
      String echoToAlias) {
    this.query = query;
    this.message = message;
    this.preferPrivate = preferPrivate;
    this.connectionType = connectionType;
    this.echoToAlias = echoToAlias;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getQuery() {
    return query;
  }

  public void setQuery(String query) {
    this.query = query;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public Boolean getPreferPrivate() {
    return preferPrivate;
  }

  public void setPreferPrivate(Boolean preferPrivate) {
    this.preferPrivate = preferPrivate;
  }

  public String getConnectionType() {
    return connectionType;
  }

  public void setConnectionType(String connectionType) {
    this.connectionType = connectionType;
  }

  public String getEchoToAlias() {
    return echoToAlias;
  }

  public void setEchoToAlias(String echoToAlias) {
    this.echoToAlias = echoToAlias;
  }

  public static class Builder {
    private String query;
    private String message;
    private Boolean preferPrivate;
    private String connectionType;
    private String echoToAlias;

    public Builder query(String query) {
      this.query = query;
      return this;
    }

    public Builder message(String message) {
      this.message = message;
      return this;
    }

    public Builder preferPrivate(Boolean preferPrivate) {
      this.preferPrivate = preferPrivate;
      return this;
    }

    public Builder connectionType(String connectionType) {
      this.connectionType = connectionType;
      return this;
    }

    public Builder echoToAlias(String echoToAlias) {
      this.echoToAlias = echoToAlias;
      return this;
    }

    public SendMessageToKnownUserRequest build() {
      return new SendMessageToKnownUserRequest(query, message, preferPrivate, connectionType, echoToAlias);
    }
  }
}

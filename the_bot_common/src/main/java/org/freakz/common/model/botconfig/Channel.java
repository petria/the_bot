package org.freakz.common.model.botconfig;

import java.util.List;
import java.util.Objects;

public class Channel {

  private String id;
  private String description;
  private String name;
  private String type;
  private String echoToAlias;
  private List<String> echoToAliases;
  private boolean joinOnStart;
  private Boolean publicAiEnabled;
  private Boolean allowAnonymousAiCommands;
  private Boolean resolveUrls;
  private Boolean alertMessages;
  private Boolean captureResolvedUrls;
  private Boolean captureImages;
  private List<String> captureImageToAliases;

  public Channel() {
  }

  public Channel(String id, String description, String name, String type, String echoToAlias, List<String> echoToAliases, boolean joinOnStart, Boolean publicAiEnabled) {
    this(id, description, name, type, echoToAlias, echoToAliases, joinOnStart, publicAiEnabled, null, null, null, null, null);
  }

  public Channel(String id, String description, String name, String type, String echoToAlias, List<String> echoToAliases, boolean joinOnStart, Boolean publicAiEnabled, Boolean allowAnonymousAiCommands) {
    this(id, description, name, type, echoToAlias, echoToAliases, joinOnStart, publicAiEnabled, allowAnonymousAiCommands, null, null, null, null);
  }

  public Channel(String id, String description, String name, String type, String echoToAlias, List<String> echoToAliases, boolean joinOnStart, Boolean publicAiEnabled, Boolean allowAnonymousAiCommands, Boolean resolveUrls) {
    this(id, description, name, type, echoToAlias, echoToAliases, joinOnStart, publicAiEnabled, allowAnonymousAiCommands, resolveUrls, null, null, null);
  }

  public Channel(String id, String description, String name, String type, String echoToAlias, List<String> echoToAliases, boolean joinOnStart, Boolean publicAiEnabled, Boolean allowAnonymousAiCommands, Boolean resolveUrls, Boolean alertMessages) {
    this(id, description, name, type, echoToAlias, echoToAliases, joinOnStart, publicAiEnabled, allowAnonymousAiCommands, resolveUrls, alertMessages, null, null);
  }

  public Channel(String id, String description, String name, String type, String echoToAlias, List<String> echoToAliases, boolean joinOnStart, Boolean publicAiEnabled, Boolean allowAnonymousAiCommands, Boolean resolveUrls, Boolean alertMessages, Boolean captureImages, List<String> captureImageToAliases) {
    this(id, description, name, type, echoToAlias, echoToAliases, joinOnStart, publicAiEnabled, allowAnonymousAiCommands, resolveUrls, alertMessages, null, captureImages, captureImageToAliases);
  }

  public Channel(String id, String description, String name, String type, String echoToAlias, List<String> echoToAliases, boolean joinOnStart, Boolean publicAiEnabled, Boolean allowAnonymousAiCommands, Boolean resolveUrls, Boolean alertMessages, Boolean captureResolvedUrls, Boolean captureImages, List<String> captureImageToAliases) {
    this.id = id;
    this.description = description;
    this.name = name;
    this.type = type;
    this.echoToAlias = echoToAlias;
    this.echoToAliases = echoToAliases;
    this.joinOnStart = joinOnStart;
    this.publicAiEnabled = publicAiEnabled;
    this.allowAnonymousAiCommands = allowAnonymousAiCommands;
    this.resolveUrls = resolveUrls;
    this.alertMessages = alertMessages;
    this.captureResolvedUrls = captureResolvedUrls;
    this.captureImages = captureImages;
    this.captureImageToAliases = captureImageToAliases;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getEchoToAlias() {
    return echoToAlias;
  }

  public void setEchoToAlias(String echoToAlias) {
    this.echoToAlias = echoToAlias;
  }

  public List<String> getEchoToAliases() {
    return echoToAliases;
  }

  public void setEchoToAliases(List<String> echoToAliases) {
    this.echoToAliases = echoToAliases;
  }

  public boolean isJoinOnStart() {
    return joinOnStart;
  }

  public void setJoinOnStart(boolean joinOnStart) {
    this.joinOnStart = joinOnStart;
  }

  public Boolean getPublicAiEnabled() {
    return publicAiEnabled;
  }

  public void setPublicAiEnabled(Boolean publicAiEnabled) {
    this.publicAiEnabled = publicAiEnabled;
  }

  public Boolean getAllowAnonymousAiCommands() {
    return allowAnonymousAiCommands;
  }

  public void setAllowAnonymousAiCommands(Boolean allowAnonymousAiCommands) {
    this.allowAnonymousAiCommands = allowAnonymousAiCommands;
  }

  public Boolean getResolveUrls() {
    return resolveUrls;
  }

  public void setResolveUrls(Boolean resolveUrls) {
    this.resolveUrls = resolveUrls;
  }

  public Boolean getAlertMessages() {
    return alertMessages;
  }

  public void setAlertMessages(Boolean alertMessages) {
    this.alertMessages = alertMessages;
  }

  public Boolean getCaptureResolvedUrls() {
    return captureResolvedUrls;
  }

  public void setCaptureResolvedUrls(Boolean captureResolvedUrls) {
    this.captureResolvedUrls = captureResolvedUrls;
  }

  public Boolean getCaptureImages() {
    return captureImages;
  }

  public void setCaptureImages(Boolean captureImages) {
    this.captureImages = captureImages;
  }

  public List<String> getCaptureImageToAliases() {
    return captureImageToAliases;
  }

  public void setCaptureImageToAliases(List<String> captureImageToAliases) {
    this.captureImageToAliases = captureImageToAliases;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Channel channel = (Channel) o;
    return joinOnStart == channel.joinOnStart && Objects.equals(publicAiEnabled, channel.publicAiEnabled) && Objects.equals(allowAnonymousAiCommands, channel.allowAnonymousAiCommands) && Objects.equals(resolveUrls, channel.resolveUrls) && Objects.equals(alertMessages, channel.alertMessages) && Objects.equals(captureResolvedUrls, channel.captureResolvedUrls) && Objects.equals(captureImages, channel.captureImages) && Objects.equals(id, channel.id) && Objects.equals(description, channel.description) && Objects.equals(name, channel.name) && Objects.equals(type, channel.type) && Objects.equals(echoToAlias, channel.echoToAlias) && Objects.equals(echoToAliases, channel.echoToAliases) && Objects.equals(captureImageToAliases, channel.captureImageToAliases);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, description, name, type, echoToAlias, echoToAliases, joinOnStart, publicAiEnabled, allowAnonymousAiCommands, resolveUrls, alertMessages, captureResolvedUrls, captureImages, captureImageToAliases);
  }

  @Override
  public String toString() {
    return "Channel{" +
        "id='" + id + '\'' +
        ", description='" + description + '\'' +
        ", name='" + name + '\'' +
        ", type='" + type + '\'' +
        ", echoToAlias='" + echoToAlias + '\'' +
        ", echoToAliases=" + echoToAliases +
        ", joinOnStart=" + joinOnStart +
        ", publicAiEnabled=" + publicAiEnabled +
        ", allowAnonymousAiCommands=" + allowAnonymousAiCommands +
        ", resolveUrls=" + resolveUrls +
        ", alertMessages=" + alertMessages +
        ", captureResolvedUrls=" + captureResolvedUrls +
        ", captureImages=" + captureImages +
        ", captureImageToAliases=" + captureImageToAliases +
        '}';
  }

  public static class Builder {
    private String id;
    private String description;
    private String name;
    private String type;
    private String echoToAlias;
    private List<String> echoToAliases;
    private boolean joinOnStart;
    private Boolean publicAiEnabled;
    private Boolean allowAnonymousAiCommands;
    private Boolean resolveUrls;
    private Boolean alertMessages;
    private Boolean captureResolvedUrls;
    private Boolean captureImages;
    private List<String> captureImageToAliases;

    public Builder id(String id) {
      this.id = id;
      return this;
    }

    public Builder description(String description) {
      this.description = description;
      return this;
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder type(String type) {
      this.type = type;
      return this;
    }

    public Builder echoToAlias(String echoToAlias) {
      this.echoToAlias = echoToAlias;
      return this;
    }

    public Builder echoToAliases(List<String> echoToAliases) {
      this.echoToAliases = echoToAliases;
      return this;
    }

    public Builder joinOnStart(boolean joinOnStart) {
      this.joinOnStart = joinOnStart;
      return this;
    }

    public Builder publicAiEnabled(Boolean publicAiEnabled) {
      this.publicAiEnabled = publicAiEnabled;
      return this;
    }

    public Builder allowAnonymousAiCommands(Boolean allowAnonymousAiCommands) {
      this.allowAnonymousAiCommands = allowAnonymousAiCommands;
      return this;
    }

    public Builder resolveUrls(Boolean resolveUrls) {
      this.resolveUrls = resolveUrls;
      return this;
    }

    public Builder alertMessages(Boolean alertMessages) {
      this.alertMessages = alertMessages;
      return this;
    }

    public Builder captureResolvedUrls(Boolean captureResolvedUrls) {
      this.captureResolvedUrls = captureResolvedUrls;
      return this;
    }

    public Builder captureImages(Boolean captureImages) {
      this.captureImages = captureImages;
      return this;
    }

    public Builder captureImageToAliases(List<String> captureImageToAliases) {
      this.captureImageToAliases = captureImageToAliases;
      return this;
    }

    public Channel build() {
      return new Channel(id, description, name, type, echoToAlias, echoToAliases, joinOnStart, publicAiEnabled, allowAnonymousAiCommands, resolveUrls, alertMessages, captureResolvedUrls, captureImages, captureImageToAliases);
    }
  }
}

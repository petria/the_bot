package org.freakz.common.model.engine;

import java.util.Objects;

public class BuildInfo {

  private String userName;
  private String userTimezone;
  private String javaVmVendor;
  private String javaVmVersion;
  private String javaVmName;
  private String javaRuntimeVersion;
  private String osName;
  private String osVersion;
  private String osArch;
  private String javaHome;
  private String buildTime;
  private String gitRevision;
  private String gitUrl;

  public BuildInfo() {
  }

  public BuildInfo(String userName, String userTimezone, String javaVmVendor, String javaVmVersion, String javaVmName, String javaRuntimeVersion, String osName, String osVersion, String osArch, String javaHome, String buildTime, String gitRevision, String gitUrl) {
    this.userName = userName;
    this.userTimezone = userTimezone;
    this.javaVmVendor = javaVmVendor;
    this.javaVmVersion = javaVmVersion;
    this.javaVmName = javaVmName;
    this.javaRuntimeVersion = javaRuntimeVersion;
    this.osName = osName;
    this.osVersion = osVersion;
    this.osArch = osArch;
    this.javaHome = javaHome;
    this.buildTime = buildTime;
    this.gitRevision = gitRevision;
    this.gitUrl = gitUrl;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public String getUserTimezone() {
    return userTimezone;
  }

  public void setUserTimezone(String userTimezone) {
    this.userTimezone = userTimezone;
  }

  public String getJavaVmVendor() {
    return javaVmVendor;
  }

  public void setJavaVmVendor(String javaVmVendor) {
    this.javaVmVendor = javaVmVendor;
  }

  public String getJavaVmVersion() {
    return javaVmVersion;
  }

  public void setJavaVmVersion(String javaVmVersion) {
    this.javaVmVersion = javaVmVersion;
  }

  public String getJavaVmName() {
    return javaVmName;
  }

  public void setJavaVmName(String javaVmName) {
    this.javaVmName = javaVmName;
  }

  public String getJavaRuntimeVersion() {
    return javaRuntimeVersion;
  }

  public void setJavaRuntimeVersion(String javaRuntimeVersion) {
    this.javaRuntimeVersion = javaRuntimeVersion;
  }

  public String getOsName() {
    return osName;
  }

  public void setOsName(String osName) {
    this.osName = osName;
  }

  public String getOsVersion() {
    return osVersion;
  }

  public void setOsVersion(String osVersion) {
    this.osVersion = osVersion;
  }

  public String getOsArch() {
    return osArch;
  }

  public void setOsArch(String osArch) {
    this.osArch = osArch;
  }

  public String getJavaHome() {
    return javaHome;
  }

  public void setJavaHome(String javaHome) {
    this.javaHome = javaHome;
  }

  public String getBuildTime() {
    return buildTime;
  }

  public void setBuildTime(String buildTime) {
    this.buildTime = buildTime;
  }

  public String getGitRevision() {
    return gitRevision;
  }

  public void setGitRevision(String gitRevision) {
    this.gitRevision = gitRevision;
  }

  public String getGitUrl() {
    return gitUrl;
  }

  public void setGitUrl(String gitUrl) {
    this.gitUrl = gitUrl;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    BuildInfo buildInfo = (BuildInfo) o;
    return Objects.equals(userName, buildInfo.userName) && Objects.equals(userTimezone, buildInfo.userTimezone) && Objects.equals(javaVmVendor, buildInfo.javaVmVendor) && Objects.equals(javaVmVersion, buildInfo.javaVmVersion) && Objects.equals(javaVmName, buildInfo.javaVmName) && Objects.equals(javaRuntimeVersion, buildInfo.javaRuntimeVersion) && Objects.equals(osName, buildInfo.osName) && Objects.equals(osVersion, buildInfo.osVersion) && Objects.equals(osArch, buildInfo.osArch) && Objects.equals(javaHome, buildInfo.javaHome) && Objects.equals(buildTime, buildInfo.buildTime) && Objects.equals(gitRevision, buildInfo.gitRevision) && Objects.equals(gitUrl, buildInfo.gitUrl);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userName, userTimezone, javaVmVendor, javaVmVersion, javaVmName, javaRuntimeVersion, osName, osVersion, osArch, javaHome, buildTime, gitRevision, gitUrl);
  }

  @Override
  public String toString() {
    return "BuildInfo{" +
        "userName='" + userName + '\'' +
        ", userTimezone='" + userTimezone + '\'' +
        ", javaVmVendor='" + javaVmVendor + '\'' +
        ", javaVmVersion='" + javaVmVersion + '\'' +
        ", javaVmName='" + javaVmName + '\'' +
        ", javaRuntimeVersion='" + javaRuntimeVersion + '\'' +
        ", osName='" + osName + '\'' +
        ", osVersion='" + osVersion + '\'' +
        ", osArch='" + osArch + '\'' +
        ", javaHome='" + javaHome + '\'' +
        ", buildTime='" + buildTime + '\'' +
        ", gitRevision='" + gitRevision + '\'' +
        ", gitUrl='" + gitUrl + '\'' +
        '}';
  }

  public static class Builder {
    private String userName;
    private String userTimezone;
    private String javaVmVendor;
    private String javaVmVersion;
    private String javaVmName;
    private String javaRuntimeVersion;
    private String osName;
    private String osVersion;
    private String osArch;
    private String javaHome;
    private String buildTime;
    private String gitRevision;
    private String gitUrl;

    public Builder userName(String userName) {
      this.userName = userName;
      return this;
    }

    public Builder userTimezone(String userTimezone) {
      this.userTimezone = userTimezone;
      return this;
    }

    public Builder javaVmVendor(String javaVmVendor) {
      this.javaVmVendor = javaVmVendor;
      return this;
    }

    public Builder javaVmVersion(String javaVmVersion) {
      this.javaVmVersion = javaVmVersion;
      return this;
    }

    public Builder javaVmName(String javaVmName) {
      this.javaVmName = javaVmName;
      return this;
    }

    public Builder javaRuntimeVersion(String javaRuntimeVersion) {
      this.javaRuntimeVersion = javaRuntimeVersion;
      return this;
    }

    public Builder osName(String osName) {
      this.osName = osName;
      return this;
    }

    public Builder osVersion(String osVersion) {
      this.osVersion = osVersion;
      return this;
    }

    public Builder osArch(String osArch) {
      this.osArch = osArch;
      return this;
    }

    public Builder javaHome(String javaHome) {
      this.javaHome = javaHome;
      return this;
    }

    public Builder buildTime(String buildTime) {
      this.buildTime = buildTime;
      return this;
    }

    public Builder gitRevision(String gitRevision) {
      this.gitRevision = gitRevision;
      return this;
    }

    public Builder gitUrl(String gitUrl) {
      this.gitUrl = gitUrl;
      return this;
    }

    public BuildInfo build() {
      return new BuildInfo(userName, userTimezone, javaVmVendor, javaVmVersion, javaVmName, javaRuntimeVersion, osName, osVersion, osArch, javaHome, buildTime, gitRevision, gitUrl);
    }
  }
}

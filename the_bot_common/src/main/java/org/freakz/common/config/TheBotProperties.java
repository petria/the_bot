package org.freakz.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "the.bot")
public class TheBotProperties {

  private String configFile;
  private String dataDir;
  private String runtimeDir;
  private String secretPropertiesFile;
  private String logDir;

  public TheBotProperties() {
  }

  public TheBotProperties(String configFile, String dataDir, String runtimeDir, String secretPropertiesFile, String logDir) {
    this.configFile = configFile;
    this.dataDir = dataDir;
    this.runtimeDir = runtimeDir;
    this.secretPropertiesFile = secretPropertiesFile;
    this.logDir = logDir;
  }

  public String getConfigFile() {
    return configFile;
  }

  public void setConfigFile(String configFile) {
    this.configFile = configFile;
  }

  public String getDataDir() {
    return dataDir;
  }

  public void setDataDir(String dataDir) {
    this.dataDir = dataDir;
  }

  public String getRuntimeDir() {
    return runtimeDir;
  }

  public void setRuntimeDir(String runtimeDir) {
    this.runtimeDir = runtimeDir;
  }

  public String getSecretPropertiesFile() {
    return secretPropertiesFile;
  }

  public void setSecretPropertiesFile(String secretPropertiesFile) {
    this.secretPropertiesFile = secretPropertiesFile;
  }

  public String getLogDir() {
    return logDir;
  }

  public void setLogDir(String logDir) {
    this.logDir = logDir;
  }

  @Override
  public String toString() {
    return "TheBotProperties{" +
        "configFile='" + configFile + '\'' +
        ", dataDir='" + dataDir + '\'' +
        ", runtimeDir='" + runtimeDir + '\'' +
        ", secretPropertiesFile='" + secretPropertiesFile + '\'' +
        ", logDir='" + logDir + '\'' +
        '}';
  }
}

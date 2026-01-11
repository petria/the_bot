package org.freakz.engine.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "the.bot")
public class TheBotProperties {

  private String dataDir;
  private String runtimeDir;

  private String secretPropertiesFile;

  private String logDir;

  public TheBotProperties() {
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
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    TheBotProperties that = (TheBotProperties) o;

    if (dataDir != null ? !dataDir.equals(that.dataDir) : that.dataDir != null) return false;
    if (runtimeDir != null ? !runtimeDir.equals(that.runtimeDir) : that.runtimeDir != null) return false;
    if (secretPropertiesFile != null ? !secretPropertiesFile.equals(that.secretPropertiesFile) : that.secretPropertiesFile != null)
      return false;
    return logDir != null ? logDir.equals(that.logDir) : that.logDir == null;
  }

  @Override
  public int hashCode() {
    int result = dataDir != null ? dataDir.hashCode() : 0;
    result = 31 * result + (runtimeDir != null ? runtimeDir.hashCode() : 0);
    result = 31 * result + (secretPropertiesFile != null ? secretPropertiesFile.hashCode() : 0);
    result = 31 * result + (logDir != null ? logDir.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "TheBotProperties{" +
        "dataDir='" + dataDir + '\'' +
        ", runtimeDir='" + runtimeDir + '\'' +
        ", secretPropertiesFile='" + secretPropertiesFile + '\'' +
        ", logDir='" + logDir + '\'' +
        '}';
  }

}

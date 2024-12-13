package org.freakz.common.model.engine;

import lombok.*;

@AllArgsConstructor
@Builder
@Data
@NoArgsConstructor
@ToString
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
}

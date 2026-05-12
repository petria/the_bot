package org.freakz.web.system;

public interface ContainerStatusProvider {

  ContainerStatus getStatus(String containerName);
}

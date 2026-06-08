package org.freakz.hermesmanager.service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import org.freakz.hermesmanager.config.HermesManagerProperties;
import org.springframework.stereotype.Service;

@Service
public class HermesGatewayService {

  private final DockerClient dockerClient;
  private final HermesManagerProperties properties;

  public HermesGatewayService(DockerClient dockerClient, HermesManagerProperties properties) {
    this.dockerClient = dockerClient;
    this.properties = properties;
  }

  public void restart(String profile) {
    String service = "/run/service/gateway-" + profile;
    var exec = dockerClient.execCreateCmd(properties.containerName())
        .withAttachStderr(true)
        .withAttachStdout(true)
        .withCmd("/package/admin/s6-2.15.0.0/command/s6-svc", "-r", service)
        .exec();
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    try {
      dockerClient.execStartCmd(exec.getId())
          .exec(new ExecStartResultCallback() {
            @Override
            public void onNext(Frame frame) {
              output.writeBytes(frame.getPayload());
              super.onNext(frame);
            }
          })
          .awaitCompletion();
      Long exitCode = dockerClient.inspectExecCmd(exec.getId()).exec().getExitCodeLong();
      if (exitCode == null || exitCode != 0) {
        throw new IllegalStateException("Could not restart Hermes profile " + profile + ": "
            + output.toString(StandardCharsets.UTF_8));
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while restarting Hermes profile " + profile, e);
    }
  }
}

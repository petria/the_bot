package org.freakz.engine.services.notifications;

import org.freakz.engine.config.ConfigService;
import org.freakz.engine.dto.AlertResponse;
import org.freakz.engine.services.api.AbstractService;
import org.freakz.engine.services.api.ServiceMessageHandler;
import org.freakz.engine.services.api.ServiceRequest;
import org.freakz.engine.services.api.ServiceRequestType;

import java.util.ArrayList;
import java.util.Set;

import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_MESSAGE;

@ServiceMessageHandler(ServiceRequestType = ServiceRequestType.SendAlertToNotifyChannels)
public class SendAlertToNotifyChannelsService extends AbstractService {

  @Override
  public void initializeService(ConfigService configService) {
  }

  @Override
  public <T extends org.freakz.engine.services.api.ServiceResponse> AlertResponse handleServiceRequest(ServiceRequest request) {
    PrivateChatAlertService privateChatAlertService =
        request.getApplicationContext().getBean(PrivateChatAlertService.class);
    String[] parts = request.getResults().getStringArray(ARG_MESSAGE);
    String message = parts == null ? "" : String.join(" ", parts);

    Set<String> sentTargets = privateChatAlertService.sendAlertToConfiguredTargets(message);
    AlertResponse response = AlertResponse.builder()
        .sentTo(new ArrayList<>(sentTargets))
        .build();
    response.setStatus(sentTargets.isEmpty() ? "NOK: no notify targets configured" : "OK");
    return response;
  }
}

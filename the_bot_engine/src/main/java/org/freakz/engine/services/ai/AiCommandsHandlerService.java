package org.freakz.engine.services.ai;

import org.freakz.engine.commands.util.CommandArgs;
import org.freakz.engine.dto.ai.AiResponse;
import org.freakz.engine.services.ai.claw.OpenClawAiService;
import org.freakz.engine.services.ai.hermes.HermesAiService;
import org.freakz.engine.services.api.ServiceMessageHandlerMethod;
import org.freakz.engine.services.api.ServiceRequest;
import org.freakz.engine.services.api.ServiceRequestType;
import org.freakz.engine.services.api.SpringServiceMethodHandler;
import org.springframework.stereotype.Service;

@Service
@SpringServiceMethodHandler
public class AiCommandsHandlerService {

  private final OpenClawAiService openClawAiService;

  private final HermesAiService hermesAiService;

  public AiCommandsHandlerService(OpenClawAiService openClawAiService, HermesAiService hermesAiService) {
    this.openClawAiService = openClawAiService;
    this.hermesAiService = hermesAiService;
  }

  @ServiceMessageHandlerMethod(ServiceRequestType = ServiceRequestType.AiService)
  public AiResponse handleServiceRequest(ServiceRequest request) {

    AiResponse aiResponse = AiResponse.builder().build();
    aiResponse.setStatus("OK: AI!");

    String message = request.getEngineRequest().getMessage();
    CommandArgs args = new CommandArgs(message);
    String queryMessage = args.joinArgs(0);

    openClawAiService.ask(request.getEngineRequest(), queryMessage);

    return aiResponse;
  }

  @ServiceMessageHandlerMethod(ServiceRequestType = ServiceRequestType.HermesAiService)
  public AiResponse handleHermesServiceRequest(ServiceRequest request) {

    AiResponse aiResponse = AiResponse.builder().build();
    aiResponse.setStatus("OK: Hermes AI!");

    String message = request.getEngineRequest().getMessage();
    CommandArgs args = new CommandArgs(message);
    String queryMessage = args.joinArgs(0);

    hermesAiService.ask(request.getEngineRequest(), queryMessage);

    return aiResponse;
  }
}

package org.freakz.engine.services.ollama;

import org.freakz.engine.commands.util.CommandArgs;
import org.freakz.engine.dto.ai.AiCtrlResponse;
import org.freakz.engine.dto.ai.AiResponse;
import org.freakz.engine.services.api.*;
import org.springframework.stereotype.Service;

@Service
@SpringServiceMethodHandler
public class AiCommandsHandlerService {

  private final OllamaChatService ollamaChatService;

  public AiCommandsHandlerService(OllamaChatService ollamaChatService) {
    this.ollamaChatService = ollamaChatService;
  }

  @ServiceMessageHandlerMethod(ServiceRequestType = ServiceRequestType.AiService)
  public <T extends ServiceResponse> AiResponse handleServiceRequest(ServiceRequest request) {


    AiResponse aiResponse = AiResponse.builder().build();
    aiResponse.setStatus("OK: AI!");

    String message = request.getEngineRequest().getMessage();
    CommandArgs args = new CommandArgs(message);
    String queryMessage = args.joinArgs(0);

    String network = request.getEngineRequest().getNetwork();
    String channel = request.getEngineRequest().getReplyTo();
    String sentByNick = request.getEngineRequest().getFromSender();
    String sentByRealName = request.getEngineRequest().getUser().getName();

    String queryResponse = ollamaChatService.ask(request.getEngineRequest(), "http://localhost:11434", "llama3.1:8b", queryMessage, network, channel, sentByNick, sentByRealName);

    aiResponse.setResult(queryResponse);
    return aiResponse;
  }

  @ServiceMessageHandlerMethod(ServiceRequestType = ServiceRequestType.AiCtrlService)
  public <T extends ServiceResponse> AiCtrlResponse handleAiCtlServiceRequest(
      ServiceRequest request) {
    AiCtrlResponse aiResponse = AiCtrlResponse.builder().build();
    aiResponse.setStatus("OK: AiCtl!");
    return aiResponse;
  }

}

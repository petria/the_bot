package org.freakz.engine.commands;

import org.freakz.clients.MessageSendClient;
import org.freakz.common.model.json.engine.EngineRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;


public class CommandHandlerTest {


    private ApplicationContext applicationContext = Mockito.mock(ApplicationContext.class);

    private MessageSendClient messageSendClient = Mockito.mock(MessageSendClient.class);

    @Test
    public void testWeatherCmd() throws Exception {
        String command = "!weather oulu";
        CommandHandler commandHandler = new CommandHandler(messageSendClient);
        String reply = commandHandler.handleCommand(createMockRequest(command));
        if (reply != null) {
            System.out.printf("%s: %s\n", command, reply);
        } else {
            throw new Exception(command + ": NULL reply!");
        }

    }

    private EngineRequest createMockRequest(String line) {
        return EngineRequest.builder()
                .command(line)
                .build();

    }

}

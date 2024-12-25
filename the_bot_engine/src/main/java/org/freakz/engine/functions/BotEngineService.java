package org.freakz.engine.functions;

import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.commands.BotEngine;
import org.freakz.engine.commands.util.UserAndReply;

import java.util.function.Function;

public class BotEngineService implements Function<BotEngineService.Request, BotEngineService.Response> {


    private final BotEngine botEngine;

    public BotEngineService(BotEngine botEngine) {
        this.botEngine = botEngine;
    }

    @Override
    public Response apply(Request request) {
        EngineRequest engineRequest
                = EngineRequest.builder()
                .fromChannelId(-1L)
                .timestamp(System.currentTimeMillis())
                .command(request.request())
                .replyTo("NO_REPLY")
                .fromConnectionId(-1)
                .fromSender("HokanAi")
                .fromSenderId("NO_SENDER_ID")
                .network("BOT_INTERNAL")
                .echoToAlias("THE_BOT_INTERNAL")
                .build();

        UserAndReply userAndReply = botEngine.handleEngineRequest(engineRequest, false);

        Response response = new Response(userAndReply.getReplyMessage());
        return response;
    }

    public record Request(String request) {
    }

    public record Response(String response) {
    }
}

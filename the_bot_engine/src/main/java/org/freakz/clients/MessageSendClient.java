package org.freakz.clients;

import feign.Response;
import org.freakz.common.model.json.connectionmanager.SendMessageByTargetAliasRequest;
import org.freakz.common.model.json.feed.Message;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "messageSendClient", url = "bot-io:8090", path = "/api/hokan/io/messages")
public interface MessageSendClient {

    @PostMapping("/send/{connectionId}")
    Response sendMessage(@PathVariable int connectionId, @RequestBody Message message);


    @PostMapping("/send_message_by_target_alias")
    Response sendMessageByTargetAlias(@RequestBody SendMessageByTargetAliasRequest request);

}

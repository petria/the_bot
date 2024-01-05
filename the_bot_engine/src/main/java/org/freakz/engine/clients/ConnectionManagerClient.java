package org.freakz.engine.clients;

import feign.Response;
import org.freakz.common.model.connectionmanager.ChannelUsersByTargetAliasRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "connectionManagerClient", url = "bot-io:8090", path = "/api/hokan/io/connection_manager")
public interface ConnectionManagerClient {

    @GetMapping("/get_connection_map")
    Response getConnectionMap();

    @PostMapping("/get_channel_users_by_target_alias")
    Response getChannelUsersByTargetAlias(@RequestBody ChannelUsersByTargetAliasRequest request);

}

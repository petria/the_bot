package org.freakz.services.connections;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import lombok.extern.slf4j.Slf4j;
import org.freakz.clients.ConnectionManagerClient;
import org.freakz.common.model.json.connectionmanager.GetConnectionMapResponse;
import org.freakz.common.util.FeignUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class ConnectionManagerService {


    @Autowired
    private ConnectionManagerClient client;

    @Autowired
    private ObjectMapper objectMapper;

    public GetConnectionMapResponse getConnectionsMap() {
        Response response = client.getConnectionMap();
        Optional<GetConnectionMapResponse> responseBody = FeignUtils.getResponseBody(response, GetConnectionMapResponse.class, objectMapper);
        return responseBody.get();
    }


}

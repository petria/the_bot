package org.freakz.springboot.ui.backend.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import lombok.extern.slf4j.Slf4j;
import org.freakz.common.payload.response.PingResponse;
import org.freakz.springboot.ui.backend.clients.BotIOClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/server_config")
@Slf4j
public class ServerConfigController {

    private final BotIOClient botIOClient;

    @Autowired
    public ServerConfigController(BotIOClient botIOClient) {
        this.botIOClient = botIOClient;
    }

    public static <T> Optional<T> getResponseBody(Response response, Class<T> klass) {
        try {
            String bodyJson = new BufferedReader(new InputStreamReader(response.body().asInputStream()))
                    .lines().parallel().collect(Collectors.joining("\n"));
            return Optional.ofNullable(new ObjectMapper().readValue(bodyJson, klass));
        } catch (IOException e) {
            log.error("Error when read feign response.", e);
            return Optional.empty();
        }
    }

    @GetMapping("/")
    public ResponseEntity<?> getServerConfigs() {
        try {
            Response ping = botIOClient.getPing();
            Optional<PingResponse> responseBody = getResponseBody(ping, PingResponse.class);

/*            String json =
                    "{\n" +
                            "        \"servers\": [\n" +
                            "            {\n" +
                            "                \"id\": \"1\",\n" +
                            "                \"name\": \"serverrrr1\",\n" +
                            "                \"type\": \"IRC\",\n" +
                            "                \"status\": \"online\"\n" +
                            "            },\n" +
                            "            {\n" +
                            "                \"id\": \"2\",\n" +
                            "                \"name\": \"server2\",\n" +
                            "                \"type\": \"Telegrammm\",\n" +
                            "                \"status\": \"offLine\"\n" +
                            "            }\n" +
                            "        ]\n" +
                            "    }";*/

//            HttpStatus.INTERNAL_SERVER_ERROR.
//            return ResponseEntity.status(503).body("Something wonderful just happened!!");
            return ResponseEntity.ok(responseBody.get());

        } catch (Exception exc) {
            return  ResponseEntity.internalServerError().body("fffufufufuf");

        }
    }

}

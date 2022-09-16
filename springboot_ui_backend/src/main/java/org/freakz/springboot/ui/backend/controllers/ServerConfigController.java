package org.freakz.springboot.ui.backend.controllers;

import org.freakz.springboot.ui.backend.clients.BotIOClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/server_config")
public class ServerConfigController {

    private final BotIOClient botIOClient;

    @Autowired
    public ServerConfigController(BotIOClient botIOClient) {
        this.botIOClient = botIOClient;
    }

    @GetMapping("/")
    public String getServerConfigs() {
        ResponseEntity<?> ping = botIOClient.getPing();
        String json =
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
                "    }";

        return json;
    }

}

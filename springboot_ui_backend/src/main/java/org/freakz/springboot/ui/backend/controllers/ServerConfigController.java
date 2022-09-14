package org.freakz.springboot.ui.backend.controllers;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/server_config")
public class ServerConfigController {


    @GetMapping("/")
    public String getServerConfigs() {
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

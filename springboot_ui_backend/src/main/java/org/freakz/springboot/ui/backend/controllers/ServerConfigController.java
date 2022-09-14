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
    public List<String> getServerConfigs() {
        return Arrays.asList("fufuf", "bbababa", "totototot");
    }

}

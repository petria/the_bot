package org.freakz.engine.controller;

import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.json.engine.EngineRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hokan/engine")
@Slf4j
public class EngineController {

    @PostMapping("/")
    public ResponseEntity<?> handleEngineRequest(@RequestBody EngineRequest request) {

        return null;
    }

}

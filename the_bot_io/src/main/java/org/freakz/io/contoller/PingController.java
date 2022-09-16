package org.freakz.io.contoller;

import org.freakz.common.contoller.PingControllerBase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hokan/io/ping")
public class PingController extends PingControllerBase {

    @GetMapping()
    @Override
    public ResponseEntity<?> ping() {
        return super.ping();
    }
}

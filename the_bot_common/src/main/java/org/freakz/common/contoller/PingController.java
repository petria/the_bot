package org.freakz.common.contoller;

import org.freakz.common.payload.response.PingResponse;
import org.springframework.http.ResponseEntity;

public class PingController {

    private static long START_TIME = System.currentTimeMillis();

    ResponseEntity<?> ping() {
        PingResponse response = new PingResponse();
        response.setStartTime(START_TIME);
        response.setNow(System.currentTimeMillis());
        response.setMessage("Ping response");

        return ResponseEntity.ok(response);

    }

}

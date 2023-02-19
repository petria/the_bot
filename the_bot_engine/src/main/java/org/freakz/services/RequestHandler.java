package org.freakz.services;

import lombok.Getter;
import org.freakz.dto.KelikameratResponse;

public enum RequestHandler {
    KeliKameratService(KelikameratResponse.class);

    @Getter
    private final Class<KelikameratResponse> responseClazz;

    RequestHandler(Class<KelikameratResponse> responseClazz) {
        this.responseClazz = responseClazz;
    }
}

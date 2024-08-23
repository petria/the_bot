package org.freakz.common.model.slack;

import lombok.Data;

@Data
public class UrlVerificationRequest extends UrlVerificationResponse {

    private String token;
    private String type;


}

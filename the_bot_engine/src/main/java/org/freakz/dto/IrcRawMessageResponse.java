package org.freakz.dto;

import lombok.Builder;
import lombok.Data;
import org.freakz.services.api.ServiceResponse;

import java.io.Serializable;


@Builder
@Data
public class IrcRawMessageResponse extends ServiceResponse implements Serializable {

    private String ircServerResponse;

}

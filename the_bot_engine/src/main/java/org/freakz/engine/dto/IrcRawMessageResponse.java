package org.freakz.engine.dto;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.freakz.engine.services.api.ServiceResponse;

import java.io.Serializable;


@Builder
@Data
@EqualsAndHashCode(callSuper = false)
public class IrcRawMessageResponse extends ServiceResponse implements Serializable {

    private String ircServerResponse;

}

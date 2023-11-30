package org.freakz.dto;

import lombok.Builder;
import lombok.Data;
import org.freakz.services.api.ServiceResponse;

import java.io.Serializable;


@Builder
@Data
public class ChannelUsersResponse extends ServiceResponse implements Serializable {

    private String response;

}

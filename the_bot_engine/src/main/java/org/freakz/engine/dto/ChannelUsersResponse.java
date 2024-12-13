package org.freakz.engine.dto;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.freakz.engine.services.api.ServiceResponse;

@Builder
@Data
@EqualsAndHashCode(callSuper = false)
public class ChannelUsersResponse extends ServiceResponse implements Serializable {

  private String response;
}

package org.freakz.common.model.env;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.freakz.common.model.dto.DataNodeBase;

@Builder
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class SysEnvValue extends DataNodeBase {

  @JsonProperty("keyName")
  private String keyName;

  @JsonProperty("value")
  private String value;

  @JsonProperty("modifiedBy")
  private String modifiedBy;
}

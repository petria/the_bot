package org.freakz.common.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * Orginal Date: 23.1.2012
 * Time: 11:34
 * <p>
 * Modified to the_bot 26.3.2023
 *
 * @author Petri Airio
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class DataValues extends DataNodeBase {

    @JsonProperty("nick")
    private String nick;

    @JsonProperty("network")
    private String network;


    @JsonProperty("channel")
    private String channel;

    @JsonProperty("keyName")
    private String keyName;


    @JsonProperty("value")
    private String value;

}

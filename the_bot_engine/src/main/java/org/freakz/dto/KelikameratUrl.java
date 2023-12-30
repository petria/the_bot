package org.freakz.dto;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * Created by Petri Airio on 22.6.2015.
 */
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class KelikameratUrl implements Serializable {

    public KelikameratUrl(String areaUrl, String stationUrl) {
        this.areaUrl = areaUrl;
        this.stationUrl = stationUrl;
    }

    private String areaUrl;

    private String stationUrl;

}

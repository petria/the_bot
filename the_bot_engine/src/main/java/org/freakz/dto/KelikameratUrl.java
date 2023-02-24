package org.freakz.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * Created by Petri Airio on 22.6.2015.
 */
@Data
@Builder
public class KelikameratUrl implements Serializable {

    private String areaUrl;

    private String stationUrl;

}

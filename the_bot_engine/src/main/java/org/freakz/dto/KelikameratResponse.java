package org.freakz.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class KelikameratResponse implements Serializable {


    private String status = "NOK: not implemented";

    private List<KelikameratWeatherData> dataList;


}

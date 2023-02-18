package org.freakz.dto;

import lombok.Data;
import org.freakz.services.ServiceResponse;

import java.io.Serializable;
import java.util.List;

@Data
public class KelikameratResponse extends ServiceResponse implements Serializable {


    private String status = "NOK: not implemented";

    private List<KelikameratWeatherData> dataList;


}

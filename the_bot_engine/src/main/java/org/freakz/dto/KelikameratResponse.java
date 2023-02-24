package org.freakz.dto;

import lombok.Builder;
import lombok.Data;
import org.freakz.services.ServiceResponse;

import java.io.Serializable;
import java.util.List;

@Builder
@Data
public class KelikameratResponse extends ServiceResponse implements Serializable {


    private List<KelikameratWeatherData> dataList;


}

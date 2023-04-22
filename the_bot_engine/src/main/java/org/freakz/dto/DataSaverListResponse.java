package org.freakz.dto;

import lombok.Builder;
import lombok.Data;
import org.freakz.data.repository.DataSaverInfo;
import org.freakz.services.ServiceResponse;

import java.util.ArrayList;
import java.util.List;

@Builder
@Data
public class DataSaverListResponse extends ServiceResponse {

    private List<DataSaverInfo> dataSaverInfoList;

}

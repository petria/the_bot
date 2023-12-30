package org.freakz.dto;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.freakz.data.repository.DataSaverInfo;
import org.freakz.services.api.ServiceResponse;

import java.util.List;

@Builder
@Data
@EqualsAndHashCode(callSuper = false)
public class DataSaverListResponse extends ServiceResponse {

    private List<DataSaverInfo> dataSaverInfoList;

}

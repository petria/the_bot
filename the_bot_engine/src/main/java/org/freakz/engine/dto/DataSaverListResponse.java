package org.freakz.engine.dto;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.freakz.engine.data.repository.DataSaverInfo;
import org.freakz.engine.services.api.ServiceResponse;

import java.util.List;

@Builder
@Data
@EqualsAndHashCode(callSuper = false)
public class DataSaverListResponse extends ServiceResponse {

    private List<DataSaverInfo> dataSaverInfoList;

}

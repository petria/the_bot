package org.freakz.dto;

import lombok.Builder;
import lombok.Data;
import org.freakz.common.model.json.foreca.ForecaData;
import org.freakz.services.ServiceResponse;

import java.io.Serializable;
import java.util.List;

@Builder
@Data
public class ForecaResponse extends ServiceResponse implements Serializable {

    private List<ForecaData> forecaDataList;

}

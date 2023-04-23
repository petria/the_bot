package org.freakz.common.data.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;



@Data
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DataContainerBase {

    protected final LocalDateTime lastSaved;

    public DataContainerBase() {
        lastSaved = LocalDateTime.now();
    }

}

package org.freakz.common.data.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;


@Data
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DataContainerBase {

    protected final LocalDateTime lastSaved;

    protected Integer saveTimes = 0;

    public DataContainerBase() {
        lastSaved = LocalDateTime.now();
    }

}

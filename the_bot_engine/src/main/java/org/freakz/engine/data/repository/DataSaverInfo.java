package org.freakz.engine.data.repository;


import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class DataSaverInfo {

    private String name;

    private int nodeCount;

}

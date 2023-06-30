package org.freakz.data.repository;


import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class DataSaverInfo {

    private String name;

    private int nodeCount;

}

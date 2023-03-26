package org.freakz.common.storage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Orginal Date: 23.1.2012
 * Time: 11:34
 * <p>
 * Modified to the_bot 26.3.2023
 *
 * @author Petri Airio
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataValues implements Serializable {

    private long id;

    private String nick;


    private String network;


    private String channel;


    private String keyName;


    private String value;

}

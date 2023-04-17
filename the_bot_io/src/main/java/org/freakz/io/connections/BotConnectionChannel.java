package org.freakz.io.connections;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.freakz.common.model.json.botconfig.Channel;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class BotConnectionChannel {

    private String id;

    private String targetAlias;

    private String type;

    private String network;

    private String name;

    private Channel configChannel;

}

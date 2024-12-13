package org.freakz.common.model.connectionmanager;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChannelUsersByTargetAliasResponse {


    private List<ChannelUser> channelUsers;

}

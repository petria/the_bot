package org.freakz.common.model.connectionmanager;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChannelUsersByTargetAliasResponse {


    private List<ChannelUser> channelUsers;

}

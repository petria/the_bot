package org.freakz.common.model.connectionmanager;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChannelUsersByTargetAliasRequest {

    private String targetAlias;
}

package org.freakz.common.model.users;

import lombok.*;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;

@AllArgsConstructor
@Builder
@Data
@NoArgsConstructor
@ToString
public class GetUsersRequest {

    private long timestamp;

}

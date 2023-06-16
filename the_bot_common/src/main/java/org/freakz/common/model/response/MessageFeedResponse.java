package org.freakz.common.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.freakz.common.model.feed.Message;

import java.util.List;


@AllArgsConstructor
@Builder
@Data
@NoArgsConstructor
public class MessageFeedResponse {

    private List<Message> messages;

}

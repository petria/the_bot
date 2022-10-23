package org.freakz.common.payload.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.freakz.common.model.json.feed.Message;

import java.util.List;


@AllArgsConstructor
@Builder
@Data
@NoArgsConstructor
public class MessageFeedResponse {

    private List<Message> messages;

}

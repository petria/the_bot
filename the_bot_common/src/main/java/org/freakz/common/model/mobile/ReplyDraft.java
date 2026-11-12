package org.freakz.common.model.mobile;

/** Immutable source routing captured when the notification matched. */
public record ReplyDraft(String id, String network, String channel, String sender,
                         String sourceMessage, String text, String expectedNick) {}

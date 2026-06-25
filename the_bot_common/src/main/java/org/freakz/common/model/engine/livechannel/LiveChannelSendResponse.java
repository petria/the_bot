package org.freakz.common.model.engine.livechannel;

public record LiveChannelSendResponse(boolean sent, String sentTo, String message) {
}

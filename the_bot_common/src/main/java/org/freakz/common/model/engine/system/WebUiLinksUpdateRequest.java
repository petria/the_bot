package org.freakz.common.model.engine.system;

/** Operator edit of the Web UI links. A blank value clears the override and restores the default. */
public record WebUiLinksUpdateRequest(String mainUrl, String developUrl) {
}

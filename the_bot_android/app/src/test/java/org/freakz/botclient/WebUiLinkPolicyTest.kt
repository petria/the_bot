package org.freakz.botclient

import org.junit.Assert.*
import org.junit.Test

class WebUiLinkPolicyTest {
    @Test fun onlyAbsoluteHttpAndHttpsUrlsCanBeOpened() {
        assertTrue(WebUiLinkPolicy.canOpen("https://hokanthebot.airiot.fi"))
        assertTrue(WebUiLinkPolicy.canOpen(" http://localhost:8091 "))
        for (bad in listOf("", "  ", "javascript:alert(1)", "file:///sdcard", "/relative/path",
                "https://", "ftp://example.org", "not a url")) {
            assertFalse(bad, WebUiLinkPolicy.canOpen(bad))
        }
    }

    @Test fun matchingBuildBaseUrlLabelsThisInstance() {
        assertEquals("this instance", WebUiLinkPolicy.labelFor("https://hokanthebot.airiot.fi/", "https://hokanthebot.airiot.fi"))
        assertEquals("this instance", WebUiLinkPolicy.labelFor("https://hokanthebot.airiot.fi", "https://hokanthebot.airiot.fi/"))
        assertNotEquals("this instance", WebUiLinkPolicy.labelFor("https://hokandevbot.airiot.fi", "https://hokanthebot.airiot.fi"))
    }

    @Test fun statusLabelCombinesOriginAndThisInstance() {
        assertEquals("default — this instance",
            WebUiLinkPolicy.statusLabel(WebUiLink("https://hokanthebot.airiot.fi", false), "https://hokanthebot.airiot.fi"))
        assertEquals("custom", WebUiLinkPolicy.statusLabel(WebUiLink("https://custom.example", true), "https://hokanthebot.airiot.fi"))
        assertEquals("Invalid URL", WebUiLinkPolicy.statusLabel(WebUiLink("javascript:alert(1)", true), "https://hokanthebot.airiot.fi"))
        assertEquals("No URL configured", WebUiLinkPolicy.statusLabel(WebUiLink("", false), "https://hokanthebot.airiot.fi"))
    }
}

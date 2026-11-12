package org.freakz.botclient

import org.junit.Assert.*
import org.junit.Test

class IrssiSendPolicyTest {
    @Test fun onlineReviewedDraftCanBeSentButOfflineOrInFlightCannot() {
        assertTrue(IrssiSendPolicy.canSend("Edited reply", true, null, false))
        assertFalse(IrssiSendPolicy.canSend("Edited reply", false, null, false))
        assertFalse(IrssiSendPolicy.canSend("Edited reply", true, null, true))
        for (state in listOf("QUEUED", "CLAIMED", "SUBMITTED", "UNKNOWN_FUTURE_STATE")) {
            assertFalse(IrssiSendPolicy.canSend("Edited reply", true, state, false))
            assertFalse(IrssiSendPolicy.retry(state))
        }
    }
    @Test fun uncertainAndFailedStatesRequireExplicitRetryAndBadTextIsRejected() {
        for (state in listOf("UNCERTAIN", "EXPIRED", "FAILED")) assertTrue(IrssiSendPolicy.retry(state))
        for (text in listOf("", " ", "x\n/quit", "x\rPRIVMSG #other :evil", "x\u0001", "x".repeat(501)))
            assertFalse(IrssiSendPolicy.canSend(text, true, null, false))
        assertTrue(IrssiSendPolicy.canSend("/quit is literal text",true,null,false))
    }
}

package org.freakz.botclient

object IrssiSendPolicy {
    fun statusText(status: String) = when (status) {
        "QUEUED" -> "Waiting for Irssi"
        "CLAIMED" -> "Irssi is submitting the reply"
        "SUBMITTED" -> "Submitted through Irssi"
        "FAILED" -> "Could not send"
        "EXPIRED" -> "Expired without sending"
        "UNCERTAIN" -> "Send outcome uncertain"
        else -> "Unknown send status"
    }
    fun blocked(status: String?) = status != null && !retry(status)
    fun retry(status: String?) = status in listOf("FAILED", "EXPIRED", "UNCERTAIN")
    fun canSend(text: String, available: Boolean, status: String?, submitting: Boolean) =
        available && !submitting && !blocked(status) && text.isNotBlank() && text.length <= 500 &&
            text.none { Character.isISOControl(it.code) }
}

package org.freakz.botclient

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun IrssiDraftScreen(client: BotClient, notification: NotificationItem, close: () -> Unit, modifier: Modifier = Modifier) {
    val draft = notification.replyDraft ?: return
    var text by remember(draft.id) { mutableStateOf(draft.text) }
    var available by remember(draft.id) { mutableStateOf(false) }
    var job by remember(draft.id) { mutableStateOf<IrssiJob?>(null) }
    var error by remember(draft.id) { mutableStateOf<String?>(null) }
    var submitting by remember(draft.id) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(draft.id) {
        runCatching { job = client.irssiJobs(draft.id).lastOrNull() }
        while (true) {
            runCatching { available = client.irssiAvailability(draft.id).available }.onFailure { available = false }
            job?.takeIf { it.status in listOf("QUEUED", "CLAIMED") }?.let { current ->
                runCatching { job = client.irssiJob(current.id); error = null }.onFailure { error = "Unable to check send status" }
            }
            delay(2000)
        }
    }
    val blocked = IrssiSendPolicy.blocked(job?.status)
    val retry = IrssiSendPolicy.retry(job?.status)
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("${draft.network} / ${draft.channel}", style = MaterialTheme.typography.titleMedium)
        Text("${notification.occurredAt}\n${draft.sender}: ${draft.sourceMessage}")
        Text("Send as ${draft.expectedNick ?: "your IRC nick"}, addressed to ${draft.sender}")
        OutlinedTextField(text, { text = it }, label = { Text("Your reply") }, modifier = Modifier.fillMaxWidth(), supportingText = { Text("One line, up to 500 characters") }, enabled = !blocked && !submitting)
        Text(if (available) "Irssi connected" else "Irssi unavailable: check connection, nick and channel")
        job?.let { Text("${IrssiSendPolicy.statusText(it.status)}${it.detail?.let { detail -> " — $detail" } ?: ""}") }
        if (job?.status == "UNCERTAIN") Text("The message may already have been sent. Check the channel before retrying.")
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = {
            submitting = true; error = null
            scope.launch {
                runCatching { client.sendIrssi(draft.id, text, retry) }.onSuccess { job = it }
                    .onFailure { error = "Could not submit. Check Irssi availability and your reply text." }
                submitting = false
            }
        }, enabled = IrssiSendPolicy.canSend(text, available, job?.status, submitting)) {
            Text(if (job?.status == "UNCERTAIN") "Retry (may duplicate)" else if (retry) "Retry via Irssi" else "Send via Irssi")
        }
        Button(onClick = close) { Text("Back to inbox") }
    }
}

@Composable
fun IrssiPairingScreen(client: BotClient, modifier: Modifier = Modifier) {
    var agents by remember { mutableStateOf<List<IrssiAgent>>(emptyList()) }
    var pairing by remember { mutableStateOf<IrssiPairing?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { while (true) { runCatching { agents = client.irssiAgents() }; delay(5000) } }
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Link your Irssi session", style = MaterialTheme.typography.titleMedium)
        Button(onClick = { scope.launch { runCatching { client.irssiPairing() }.onSuccess { pairing = it; error = null }.onFailure { error = "Could not create pairing code. Check your profile IRC nick." } } }) { Text("Create pairing code") }
        pairing?.let {
            Text("Run the helper's pair command on the Irssi host. This code expires at ${it.expiresAt}.")
            SelectionContainer { Text(it.code) }
            val clipboard = LocalClipboardManager.current
            var copied by remember(it.code) { mutableStateOf(false) }
            OutlinedButton(onClick = {
                clipboard.setText(AnnotatedString(it.code))
                copied = true
            }) { Text(if (copied) "Copied" else "Copy pairing code") }
        }
        agents.forEach { agent ->
            Text("${agent.name}: ${agent.expectedNick} (${if (agent.online) "online" else "offline"})")
            Button(onClick = { scope.launch { runCatching { client.revokeIrssi(agent.id); agents = client.irssiAgents() }.onFailure { error = "Could not revoke agent" } } }) { Text("Revoke ${agent.name}") }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

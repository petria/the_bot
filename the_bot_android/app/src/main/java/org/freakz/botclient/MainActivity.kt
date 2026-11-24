package org.freakz.botclient

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var notificationId by mutableStateOf<String?>(null)
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent); setIntent(intent); notificationId = intent.getStringExtra("eventId")
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val client = BotClient.get(this)
        if (client.isLoggedIn()) WaterWatchWorker.start(this)
        notificationId = intent.getStringExtra("eventId")
        setContent { BotApp(client, notificationId) }
    }
}

@Composable
private fun BotApp(client: BotClient, notificationId: String?) {
    val sessionState by client.sessionState.collectAsState()
    MaterialTheme {
        if (sessionState == MobileSession.State.SignedIn) {
            MainScreen(client, notificationId)
        } else {
            LoginScreen(client, sessionState == MobileSession.State.Expired)
        }
    }
}

@Composable
private fun LoginScreen(client: BotClient, expired: Boolean) {
    val scope = rememberCoroutineScope(); var user by remember { mutableStateOf("") }; var password by remember { mutableStateOf("") }; var error by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("The Bot", style = MaterialTheme.typography.headlineMedium)
        if (expired) Text("Your session expired. Please sign in again.")
        OutlinedTextField(user, { user = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(password, { password = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth())
        Button({
            scope.launch { runMobileAction("Login failed", { error = it }) { client.login(user, password) } }
        }, Modifier.fillMaxWidth().padding(top = 16.dp)) { Text("Login") }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun MainScreen(client: BotClient, notificationId: String?) {
    val scope = rememberCoroutineScope(); var selected by remember { mutableStateOf("Inbox") }; var notifications by remember { mutableStateOf<List<NotificationItem>>(emptyList()) }; var command by remember { mutableStateOf("") }; var reply by remember { mutableStateOf("") }; var commandError by remember { mutableStateOf<String?>(null) }
    var opened by remember { mutableStateOf<NotificationItem?>(null) }
    var requestError by remember { mutableStateOf<String?>(null) }

    suspend fun loadInbox() {
        notifications = client.notifications().filter { it.type != "WATER_WATCH" }
    }

    LaunchedEffect(notificationId) {
        runMobileAction("Could not load inbox. Try again.", { requestError = it }) {
            loadInbox()
            if (notificationId != null) {
                opened = notifications.find { it.eventId == notificationId }
                selected = "Inbox"
            }
        }
    }
    LaunchedEffect(Unit) {
        // The inbox remains usable if push registration is unavailable.
        runMobileAction("Could not register for push notifications.", {}) { client.registerCurrentDevice() }
    }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("The Bot", style = MaterialTheme.typography.headlineMedium)
        Text("Bot instance: ${BuildConfig.BOT_WEB_BASE_URL}")
        Button({
            opened = null
            selected = "Inbox"
            scope.launch { runMobileAction("Could not load inbox. Try again.", { requestError = it }) { loadInbox() } }
        }) { Text("Inbox") }
        Button({ selected = "Irssi"; opened = null }) { Text("Irssi") }
        Button({ selected = "Console"; opened = null }) { Text("Console") }
        Button({ selected = "Profile"; opened = null }) { Text("Profile") }
        Button({ selected = "Channels"; opened = null }) { Text("Channels") }
        Button({ selected = "Watch"; opened = null }) { Text("Watch") }
        Button({ selected = "WebUi"; opened = null }) { Text("WebUi") }
        Button({
            scope.launch { runMobileAction("Could not log out. Try again.", { requestError = it }) { client.logout() } }
        }) { Text("Logout") }
        requestError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (opened?.replyDraft != null) {
            IrssiDraftScreen(client, opened!!, { opened = null }, Modifier.weight(1f))
        } else when (selected) {
            "Irssi" -> IrssiPairingScreen(client, Modifier.weight(1f))
            "Inbox" -> LazyColumn {
                items(notifications) { item ->
                    Text("${item.title}: ${item.body}", Modifier.padding(vertical = 8.dp))
                    if (item.replyDraft != null) Button({ opened = item }) { Text("Review reply") }
                    Button({
                        scope.launch {
                            runMobileAction("Could not mark the notification as read. Try again.", { requestError = it }) {
                                client.markRead(item.eventId)
                                loadInbox()
                            }
                        }
                    }) { Text("Mark read") }
                }
            }
            "Console" -> { OutlinedTextField(command, { command = it; commandError = null }, label = { Text("Command") }, modifier = Modifier.fillMaxWidth()); Button({ scope.launch { commandError = null; runCatching { client.command(command).reply.orEmpty() }.onSuccess { reply = it }.onFailure { commandError = it.message ?: it::class.simpleName ?: "Command failed" } } }) { Text("Run") }; commandError?.let { Text(it, color = MaterialTheme.colorScheme.error) }; Text(reply) }
            "Profile" -> Text("Profile and notification rules are managed by bot-web.")
            "Channels" -> Text("Channel access follows the permissions assigned to your bot user.")
            "Watch" -> {
                val ctx = androidx.compose.ui.platform.LocalContext.current
                var sync by remember { mutableStateOf(WatchLinkStore(ctx).lastSyncAt()) }
                var request by remember { mutableStateOf(WatchLinkStore(ctx).lastRequestAt()) }
                Column(Modifier.padding(top = 12.dp)) {
                    Text("Water update sent to watch: " + WatchLinkStatus.render(sync, System.currentTimeMillis()), Modifier.padding(vertical = 4.dp))
                    Text("Refresh requested by watch: " + WatchLinkStatus.render(request, System.currentTimeMillis()), Modifier.padding(vertical = 4.dp))
                    Button({ sync = WatchLinkStore(ctx).lastSyncAt(); request = WatchLinkStore(ctx).lastRequestAt() }) { Text("Refresh") }
                }
            }
            "WebUi" -> WebUiLinksScreen(client, Modifier.weight(1f))
        }
    }
}

private suspend fun runMobileAction(message: String, showError: (String?) -> Unit, action: suspend () -> Unit) {
    showError(null)
    try {
        action()
    } catch (error: CancellationException) {
        throw error
    } catch (_: MobileSession.Expired) {
        // The session state already returns the UI to sign-in.
    } catch (_: Exception) {
        showError(message)
    }
}

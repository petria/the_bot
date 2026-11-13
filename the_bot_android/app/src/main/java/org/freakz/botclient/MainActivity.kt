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
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var notificationId by mutableStateOf<String?>(null)
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent); setIntent(intent); notificationId = intent.getStringExtra("eventId")
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BotClient(this).isLoggedIn()) WaterWatchWorker.start(this)
        notificationId = intent.getStringExtra("eventId")
        setContent { BotApp(BotClient(this), notificationId) }
    }
}

@Composable
private fun BotApp(client: BotClient, notificationId: String?) {
    var loggedIn by remember { mutableStateOf(client.isLoggedIn()) }
    MaterialTheme { if (loggedIn) MainScreen(client, notificationId, onLogout = { loggedIn = false }) else LoginScreen(client) { loggedIn = true } }
}

@Composable
private fun LoginScreen(client: BotClient, onLoggedIn: () -> Unit) {
    val scope = rememberCoroutineScope(); var user by remember { mutableStateOf("") }; var password by remember { mutableStateOf("") }; var error by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("The Bot", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(user, { user = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(password, { password = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth())
        Button({ scope.launch { runCatching { client.login(user, password) }.onSuccess { onLoggedIn() }.onFailure { error = "Login failed" } } }, Modifier.fillMaxWidth().padding(top = 16.dp)) { Text("Login") }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun MainScreen(client: BotClient, notificationId: String?, onLogout: () -> Unit) {
    val scope = rememberCoroutineScope(); var selected by remember { mutableStateOf("Inbox") }; var notifications by remember { mutableStateOf<List<NotificationItem>>(emptyList()) }; var command by remember { mutableStateOf("") }; var reply by remember { mutableStateOf("") }; var commandError by remember { mutableStateOf<String?>(null) }
    var opened by remember { mutableStateOf<NotificationItem?>(null) }
    LaunchedEffect(notificationId) {
        if (notificationId != null) runCatching {
            notifications = client.notifications().filter { it.type != "WATER_WATCH" }
            opened = notifications.find { it.eventId == notificationId }; selected = "Inbox"
        }
    }
    LaunchedEffect(Unit) { runCatching { client.registerCurrentDevice(); notifications = client.notifications().filter { it.type != "WATER_WATCH" } } }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("The Bot", style = MaterialTheme.typography.headlineMedium)
        Text("Bot instance: ${BuildConfig.BOT_WEB_BASE_URL}")
        Button({ opened = null; selected = "Inbox"; scope.launch { notifications = client.notifications().filter { it.type != "WATER_WATCH" } } }) { Text("Inbox") }
        Button({ selected = "Irssi"; opened = null }) { Text("Irssi") }
        Button({ selected = "Console"; opened = null }) { Text("Console") }
        Button({ selected = "Profile"; opened = null }) { Text("Profile") }
        Button({ selected = "Channels"; opened = null }) { Text("Channels") }
        Button({ selected = "Watch"; opened = null }) { Text("Watch") }
        Button({ scope.launch { client.logout(); onLogout() } }) { Text("Logout") }
        if (opened?.replyDraft != null) {
            IrssiDraftScreen(client, opened!!, { opened = null }, Modifier.weight(1f))
        } else when (selected) {
            "Irssi" -> IrssiPairingScreen(client, Modifier.weight(1f))
            "Inbox" -> LazyColumn { items(notifications) { item -> Text("${item.title}: ${item.body}", Modifier.padding(vertical = 8.dp)); if (item.replyDraft != null) Button({ opened = item }) { Text("Review reply") }; Button({ scope.launch { client.markRead(item.eventId); notifications = client.notifications().filter { it.type != "WATER_WATCH" } } }) { Text("Mark read") } } }
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
        }
    }
}

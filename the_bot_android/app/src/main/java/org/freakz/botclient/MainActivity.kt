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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BotApp(BotClient(this)) }
    }
}

@Composable
private fun BotApp(client: BotClient) {
    var loggedIn by remember { mutableStateOf(client.isLoggedIn()) }
    MaterialTheme { if (loggedIn) MainScreen(client, onLogout = { loggedIn = false }) else LoginScreen(client) { loggedIn = true } }
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
private fun MainScreen(client: BotClient, onLogout: () -> Unit) {
    val scope = rememberCoroutineScope(); var selected by remember { mutableStateOf("Inbox") }; var notifications by remember { mutableStateOf<List<NotificationItem>>(emptyList()) }; var command by remember { mutableStateOf("") }; var reply by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { runCatching { client.registerCurrentDevice(); notifications = client.notifications() } }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("The Bot", style = MaterialTheme.typography.headlineMedium)
        Text("Bot instance: ${BuildConfig.BOT_WEB_BASE_URL}")
        Button({ selected = "Inbox"; scope.launch { notifications = client.notifications() } }) { Text("Inbox") }
        Button({ selected = "Console" }) { Text("Console") }
        Button({ selected = "Profile" }) { Text("Profile") }
        Button({ selected = "Channels" }) { Text("Channels") }
        Button({ scope.launch { client.logout(); onLogout() } }) { Text("Logout") }
        when (selected) {
            "Inbox" -> LazyColumn { items(notifications) { item -> Text("${item.title}: ${item.body}", Modifier.padding(vertical = 8.dp)); Button({ scope.launch { client.markRead(item.eventId); notifications = client.notifications() } }) { Text("Mark read") } } }
            "Console" -> { OutlinedTextField(command, { command = it }, label = { Text("Command") }, modifier = Modifier.fillMaxWidth()); Button({ scope.launch { reply = client.command(command).reply.orEmpty() } }) { Text("Run") }; Text(reply) }
            "Profile" -> Text("Profile and notification rules are managed by bot-web.")
            "Channels" -> Text("Channel access follows the permissions assigned to your bot user.")
        }
    }
}

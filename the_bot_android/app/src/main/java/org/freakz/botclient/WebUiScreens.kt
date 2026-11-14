package org.freakz.botclient

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun WebUiLinksScreen(client: BotClient, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var links by remember { mutableStateOf<WebUiLinks?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var openError by remember { mutableStateOf<String?>(null) }

    suspend fun load() {
        loading = true; error = null
        runCatching { client.webUiLinks() }
            .onSuccess { links = it }
            .onFailure { error = it.message ?: "Could not load Web UI links" }
        loading = false
    }
    LaunchedEffect(Unit) { load() }

    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Web UI links", style = MaterialTheme.typography.titleMedium)
        Text("These links come from the bot configuration, not the app build.")
        if (loading) Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) { CircularProgressIndicator() }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        openError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        links?.let { current ->
            listOf("Main instance (master)" to current.main, "Develop instance" to current.develop).forEach { (title, link) ->
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(title, style = MaterialTheme.typography.titleSmall)
                        Text(WebUiLinkPolicy.statusLabel(link, BuildConfig.BOT_WEB_BASE_URL))
                        Text(link.url, style = MaterialTheme.typography.bodySmall)
                        Button(
                            enabled = WebUiLinkPolicy.canOpen(link.url),
                            onClick = {
                                openError = null
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link.url.trim())))
                                } catch (notFound: ActivityNotFoundException) {
                                    openError = "No browser can open ${link.url}"
                                }
                            }) { Text("Open") }
                    }
                }
            }
        }
        Button(onClick = { scope.launch { load() } }, enabled = !loading) { Text("Refresh") }
    }
}

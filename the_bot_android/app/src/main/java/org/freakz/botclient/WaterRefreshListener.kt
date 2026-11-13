package org.freakz.botclient

import android.content.Context
import androidx.work.*
import com.google.android.gms.wearable.*
import kotlinx.coroutines.tasks.await

class WaterRefreshListener : WearableListenerService() {
    override fun onMessageReceived(event: MessageEvent) {
        if (event.path != "/water/refresh") return
        WatchLinkStore(this).recordRequest()
        WorkManager.getInstance(this).enqueueUniqueWork("water-forced-refresh", ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<ForcedWaterWorker>()
                .setInputData(workDataOf("node" to event.sourceNodeId)).build())
    }
}

class ForcedWaterWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val node = inputData.getString("node") ?: return Result.failure()
        val status = try {
            val client = BotClient(applicationContext)
            if (!client.isLoggedIn()) throw IllegalStateException("Sign in on your phone first")
            val payload = client.refreshWater().toString()
            val request = PutDataMapRequest.create("/water/latest").apply { dataMap.putString("water", payload) }
            Wearable.getDataClient(applicationContext).putDataItem(request.asPutDataRequest().setUrgent()).await()
            WatchLinkStore(applicationContext).recordSync()
            "Updated"
        } catch (error: Exception) { "Refresh failed. Check phone connection and login, then try again." }
        runCatching { Wearable.getMessageClient(applicationContext).sendMessage(node, "/water/refresh-result", status.toByteArray()).await() }
        return Result.success()
    }
}

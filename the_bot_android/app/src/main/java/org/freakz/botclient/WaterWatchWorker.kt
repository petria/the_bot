package org.freakz.botclient

import android.content.Context
import androidx.work.*
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class WaterWatchWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result { return try {
        val client = BotClient(applicationContext)
        if (!client.isLoggedIn()) return Result.success()
        val latest = client.notifications().filter { it.type == "WATER_WATCH" }.maxByOrNull { it.occurredAt }
        val payload = latest?.body ?: "{\"enabled\":false,\"updatedAt\":${System.currentTimeMillis()}}"
        val request = PutDataMapRequest.create("/water/latest").apply { dataMap.putString("water", payload) }
        Wearable.getDataClient(applicationContext).putDataItem(request.asPutDataRequest()).await()
        Result.success()
    } catch (e: Exception) { Result.retry() }
    }

    companion object {
        fun start(context: Context) {
            val manager = WorkManager.getInstance(context)
            val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            manager.enqueueUniquePeriodicWork("water-watch-periodic", ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<WaterWatchWorker>(1, TimeUnit.HOURS).setConstraints(constraints).build())
            manager.enqueueUniqueWork("water-watch-refresh", ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<WaterWatchWorker>().setConstraints(constraints).build())
        }
        suspend fun clear(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork("water-watch-periodic")
            WorkManager.getInstance(context).cancelUniqueWork("water-watch-refresh")
            val request = PutDataMapRequest.create("/water/latest").apply {
                dataMap.putString("water", "{\"enabled\":false,\"updatedAt\":${System.currentTimeMillis()}}")
            }
            Wearable.getDataClient(context).putDataItem(request.asPutDataRequest()).await()
        }
    }
}

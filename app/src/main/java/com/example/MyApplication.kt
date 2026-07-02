package com.example

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.worker.CurrencyUpdateWorker
import java.util.concurrent.TimeUnit

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        schedulePeriodicCurrencySync()
    }

    private fun schedulePeriodicCurrencySync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicWorkRequest = PeriodicWorkRequestBuilder<CurrencyUpdateWorker>(
            6, TimeUnit.HOURS // Fetch rates every 6 hours
        )
            .setConstraints(constraints)
            .build()

        try {
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "CurrencySyncWork",
                ExistingPeriodicWorkPolicy.KEEP, // Keep existing work to avoid resetting schedule
                periodicWorkRequest
            )
        } catch (e: Throwable) {
            // WorkManager might not be initialized or available in some environments
            e.printStackTrace()
        }
    }
}

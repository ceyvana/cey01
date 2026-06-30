package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.api.CurrencyRetrofitClient
import com.example.data.database.AppDatabase
import com.example.data.database.ExchangeRateEntity

class CurrencyUpdateWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("CurrencyUpdateWorker", "Periodic background exchange rate update started...")
        return try {
            val response = CurrencyRetrofitClient.service.getLatestRates()
            if (response.result == "success") {
                val lkrRate = response.rates["LKR"]
                if (lkrRate != null && lkrRate > 0.0) {
                    val db = AppDatabase.getDatabase(applicationContext)
                    val exchangeRate = ExchangeRateEntity(
                        currencyCode = "USD",
                        rate = lkrRate,
                        lastUpdated = System.currentTimeMillis()
                    )
                    db.exchangeRateDao().insertExchangeRate(exchangeRate)
                    Log.i("CurrencyUpdateWorker", "Successfully updated exchange rate: 1 USD = $lkrRate LKR")
                    Result.success()
                } else {
                    Log.e("CurrencyUpdateWorker", "LKR rate not found in response rates.")
                    Result.failure()
                }
            } else {
                Log.e("CurrencyUpdateWorker", "API returned failure result: ${response.result}")
                Result.failure()
            }
        } catch (e: Exception) {
            Log.e("CurrencyUpdateWorker", "Failed to update exchange rate due to error: ${e.localizedMessage}", e)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}

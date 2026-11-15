package com.example.privatbankcurrencies

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

object ExchangeRatesWorkScheduler {

    const val PERIODIC_WORK_NAME = "exchange_rates_periodic"

    private val networkConstraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun schedulePeriodicSync(context: Context) {
        val periodicRequest = PeriodicWorkRequestBuilder<ExchangeRatesSyncWorker>(
            1,
            TimeUnit.HOURS
        )
            .setConstraints(networkConstraints)
            .build()

        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest
        )
    }

    fun enqueueOneTimeSync(context: Context, date: String) {
        val oneTimeRequest = OneTimeWorkRequestBuilder<ExchangeRatesSyncWorker>()
            .setInputData(workDataOf(ExchangeRatesSyncWorker.KEY_DATE to date))
            .setConstraints(networkConstraints)
            .build()

        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            uniqueOneTimeWorkName(date),
            ExistingWorkPolicy.KEEP,
            oneTimeRequest
        )
    }

    fun uniqueOneTimeWorkName(date: String): String = "exchange_rates_$date"
}
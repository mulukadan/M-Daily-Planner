package com.example.m_dailyplanner

import android.app.Application
import androidx.work.*
import com.example.m_dailyplanner.worker.CarryForwardWorker
import java.util.*
import java.util.concurrent.TimeUnit

class PlannerApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        scheduleCarryForwardWorker()
    }

    private fun scheduleCarryForwardWorker() {
        val workManager = WorkManager.getInstance(this)

        val currentDate = Calendar.getInstance()
        val dueDate = Calendar.getInstance()

        // Set execution time to 6 AM
        dueDate.set(Calendar.HOUR_OF_DAY, 6)
        dueDate.set(Calendar.MINUTE, 0)
        dueDate.set(Calendar.SECOND, 0)

        if (dueDate.before(currentDate)) {
            dueDate.add(Calendar.HOUR_OF_DAY, 24)
        }

        val initialDelay = dueDate.timeInMillis - currentDate.timeInMillis

        val carryForwardRequest = PeriodicWorkRequestBuilder<CarryForwardWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            "CarryForwardWork",
            ExistingPeriodicWorkPolicy.KEEP,
            carryForwardRequest
        )
    }
}

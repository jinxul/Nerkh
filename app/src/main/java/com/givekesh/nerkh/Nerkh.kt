package com.givekesh.nerkh

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.work.*
import java.util.concurrent.TimeUnit

class Nerkh : AppWidgetProvider() {

    private val actionRefresh = "com.givekesh.nerkh.manual.refresh"

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action.equals(actionRefresh)) {
            val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 0)
            updateAppWidget(context, appWidgetId)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {

        for (appWidgetId in appWidgetIds)
            updateAppWidget(context, appWidgetId)

    }


    override fun onEnabled(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = AppWidgetManager.getInstance(context)
            .getAppWidgetIds(ComponentName(context, Nerkh::class.java))

        for (appWidgetId in appWidgetIds) {

            val json =
                context.assets.open("defaultJson.json").bufferedReader()
                    .use { it.readText() }

            val utils = Utils(context, appWidgetId)
            utils.refreshUI("", json, appWidgetManager)
        }
    }

    override fun onDisabled(context: Context) {

        WorkManager.getInstance(context).cancelAllWork()
    }

    companion object {

        internal fun updateAppWidget(
            context: Context,
            appWidgetId: Int
        ) {
            val workManager = WorkManager.getInstance(context)

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val data = workDataOf(AppWidgetManager.EXTRA_APPWIDGET_ID to appWidgetId)

            val request =
                PeriodicWorkRequest.Builder(DataFetcherWorker::class.java, 15, TimeUnit.MINUTES)
                    .setInputData(data)
                    .setConstraints(constraints)
                    .setBackoffCriteria(BackoffPolicy.LINEAR, 15, TimeUnit.MINUTES)
                    .build()

            workManager.enqueueUniquePeriodicWork(
                "nerkh.widget.data_fetcher",
                ExistingPeriodicWorkPolicy.REPLACE, request
            )
        }
    }
}


package com.givekesh.nerkh

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
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

            val serviceIntent = Intent(context, AppWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                putExtra("json", json)
                data = Uri.parse(this.toUri(Intent.URI_INTENT_SCHEME))
            }

            val updateIntent = Intent(context, Nerkh::class.java).apply {
                action = actionRefresh
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, appWidgetId,
                updateIntent, PendingIntent.FLAG_UPDATE_CURRENT
            )

            val configIntent = Intent(context, ConfigActivity::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val configPendingIntent = PendingIntent.getActivity(
                context, 0, configIntent, 0
            )

            RemoteViews(context.packageName, R.layout.nerkh).apply {
                setOnClickPendingIntent(
                    R.id.widget_refresh,
                    pendingIntent
                )
                setOnClickPendingIntent(R.id.widget_settings, configPendingIntent)

                setRemoteAdapter(R.id.widget_list, serviceIntent)
                setEmptyView(R.id.widget_list, R.id.empty_layout)
                appWidgetManager.updateAppWidget(appWidgetId, this)
            }
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


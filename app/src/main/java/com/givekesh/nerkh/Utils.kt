package com.givekesh.nerkh

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews

class Utils(private val context: Context, private val appWidgetId: Int) {

    fun refreshUI(
        lastUpdate: String,
        json: String,
        appWidgetManager: AppWidgetManager
    ) {
        RemoteViews(context.packageName, R.layout.nerkh).apply {

            setTextViewText(R.id.last_update, lastUpdate)

            setOnClickPendingIntent(
                R.id.widget_refresh,
                getRefreshPending()
            )
            setOnClickPendingIntent(
                R.id.widget_settings,
                getConfigPending()
            )
            setRemoteAdapter(R.id.widget_list, getServiceIntent(json))
            setEmptyView(R.id.widget_list, R.id.empty_layout)
            appWidgetManager.updateAppWidget(appWidgetId, this)
        }
    }

    private fun getRefreshPending(
    ): PendingIntent {
        val updateIntent = Intent(context, Nerkh::class.java).apply {
            action = "com.givekesh.nerkh.manual.refresh"
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }

        return PendingIntent.getBroadcast(
            context, appWidgetId,
            updateIntent, PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun getConfigPending(): PendingIntent {
        val configIntent = Intent(context, ConfigActivity::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }

        return PendingIntent.getActivity(
            context, 0, configIntent, 0
        )
    }

    private fun getServiceIntent(json: String): Intent {
        return Intent(context, AppWidgetService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra("json", json)
            data = Uri.parse(this.toUri(Intent.URI_INTENT_SCHEME))
        }
    }
}

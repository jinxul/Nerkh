package com.givekesh.nerkh

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.crashlytics.android.Crashlytics
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class DataFetcherWorker(context: Context, workerParams: WorkerParameters) : Worker(
    context,
    workerParams
) {

    override fun doWork(): Result {
        val context = applicationContext
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetId = inputData.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, 17987)
        val remoteView = RemoteViews(context.packageName, R.layout.nerkh)

        try {
            val document = Jsoup.connect("http://tgju.org/").timeout(5000).get()
            val jsonArray = JSONArray()
            val header = JSONObject()
            header.put("itemTitle", "عنوان")
            header.put("currentPrice", "قیمت")
            header.put("priceChanges", "تغییر")
            jsonArray.put(header)

            val fields = context.resources.getStringArray(R.array.fields)
            for (field in fields) {
                val elements = document.getElementById(field)
                val jsonObject = JSONObject()
                jsonObject.put("itemTitle", elements.child(0).text())
                jsonObject.put("currentPrice", elements.child(1).text())
                jsonObject.put("priceChanges", elements.child(2).text())
                jsonObject.put("changeIndicator", elements.attr("class"))
                jsonArray.put(jsonObject)
            }
            if (jsonArray.length() > 1) {
                val serviceIntent = getServiceIntent(context, appWidgetId, jsonArray.toString())
                val lastUpdate = context.resources.getString(
                    R.string.last_update,
                    SimpleDateFormat("hh:mm:ss a", Locale.US).format(Date())
                )
                refreshUI(
                    context,
                    remoteView,
                    lastUpdate,
                    appWidgetId,
                    serviceIntent,
                    appWidgetManager
                )
                return Result.success()
            }
        } catch (exception: Exception) {
            Crashlytics.logException(exception)
        }
        val json =
            context.assets.open("defaultJson.json").bufferedReader()
                .use { it.readText() }
        val serviceIntent = getServiceIntent(context, appWidgetId, json)
        refreshUI(context, remoteView, "", appWidgetId, serviceIntent, appWidgetManager)
        return Result.retry()
    }

    private fun refreshUI(
        context: Context,
        remoteView: RemoteViews,
        lastUpdate: String,
        appWidgetId: Int,
        serviceIntent: Intent,
        appWidgetManager: AppWidgetManager
    ) {
        remoteView.setTextViewText(R.id.last_update, lastUpdate)

        remoteView.setOnClickPendingIntent(
            R.id.widget_refresh,
            getRefreshPending(context, appWidgetId)
        )
        remoteView.setRemoteAdapter(R.id.widget_list, serviceIntent)
        remoteView.setEmptyView(R.id.widget_list, R.id.empty_layout)
        appWidgetManager.updateAppWidget(appWidgetId, remoteView)
    }

    private fun getRefreshPending(
        context: Context,
        appWidgetId: Int
    ): PendingIntent {
        val updateIntent = Intent(context, Nerkh::class.java)
        updateIntent.action = "com.givekesh.nerkh.manual.refresh"
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)

        return PendingIntent.getBroadcast(
            context, appWidgetId,
            updateIntent, PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun getServiceIntent(context: Context, appWidgetId: Int, json: String): Intent {
        val serviceIntent = Intent(context, AppWidgetService::class.java)
        serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        serviceIntent.putExtra("json", json)
        serviceIntent.data =
            Uri.parse(serviceIntent.toUri(Intent.URI_INTENT_SCHEME))
        return serviceIntent
    }
}
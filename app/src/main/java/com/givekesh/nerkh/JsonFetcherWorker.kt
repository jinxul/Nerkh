package com.givekesh.nerkh

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.android.volley.Response
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley
import com.crashlytics.android.Crashlytics
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*

class JsonFetcherWorker(context: Context, workerParams: WorkerParameters) : Worker(
    context,
    workerParams
) {

    override fun doWork(): Result {
        val context = applicationContext
        val queue = Volley.newRequestQueue(context)
        val url = "http://nerkh.orgfree.com/"
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetId = inputData.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, 17987)
        val remoteView = RemoteViews(context.packageName, R.layout.nerkh)

        val request = JsonArrayRequest(
            url,
            Response.Listener<JSONArray> { response ->

                val serviceIntent = getServiceIntent(context, appWidgetId, response.toString())
                val lastUpdate = context.resources.getString(
                    R.string.last_update,
                    SimpleDateFormat("hh:mm:ss a", Locale.US).format(Date())
                )
                Log.e("TAG", response.toString())

                refreshUI(
                    context,
                    remoteView,
                    lastUpdate,
                    appWidgetId,
                    serviceIntent,
                    appWidgetManager
                )
            },
            Response.ErrorListener { VolleyError ->
                if (!VolleyError.toString().contains("java.net.UnknownHostException"))
                    Crashlytics.logException(VolleyError.cause)

                val json =
                    context.assets.open("defaultJson.json").bufferedReader()
                        .use { it.readText() }
                val serviceIntent = getServiceIntent(context, appWidgetId, json)

                refreshUI(
                    context,
                    remoteView,
                    "",
                    appWidgetId,
                    serviceIntent,
                    appWidgetManager
                )

            })
        queue.add(request)

        return Result.success()
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
        remoteView.setViewVisibility(R.id.header_layout, View.VISIBLE)
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
        serviceIntent.putExtra(
            "json",
            "[{\"name\":\"عنوان\", \"price\":\"قیمت\", \"change\":\"تغییر\"}," +
                    json.replace("[", "")
        )
        serviceIntent.data =
            Uri.parse(serviceIntent.toUri(Intent.URI_INTENT_SCHEME))
        return serviceIntent
    }
}
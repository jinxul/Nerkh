package com.givekesh.nerkh

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import androidx.preference.PreferenceManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.crashlytics.android.Crashlytics
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

class DataFetcherWorker(context: Context, workerParams: WorkerParameters) : Worker(
    context,
    workerParams
) {

    override fun doWork(): Result {
        val context = applicationContext
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetId = inputData.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, 17987)

        try {
            val url = "http://tgju.org/"
            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(5000, TimeUnit.MILLISECONDS)
                .readTimeout(2500, TimeUnit.MILLISECONDS)
                .build()
            val request = Request.Builder()
                .url(url)
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.120 Safari/537.36"
                )
                .get().build()
            val html = okHttpClient.newCall(request).execute().body().string()
            val document = Jsoup.parse(html)
            val jsonArray = parseData(context, document)
            if (jsonArray.length() > 1) {
                val serviceIntent = getServiceIntent(context, appWidgetId, jsonArray.toString())
                val lastUpdate = context.resources.getString(
                    R.string.last_update,
                    SimpleDateFormat("hh:mm:ss a", Locale.US).format(Date())
                )
                refreshUI(
                    context,
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
        refreshUI(context, "", appWidgetId, serviceIntent, appWidgetManager)
        return Result.retry()
    }

    private fun refreshUI(
        context: Context,
        lastUpdate: String,
        appWidgetId: Int,
        serviceIntent: Intent,
        appWidgetManager: AppWidgetManager
    ) {
        RemoteViews(context.packageName, R.layout.nerkh).apply {
            setTextViewText(R.id.last_update, lastUpdate)

            setOnClickPendingIntent(
                R.id.widget_refresh,
                getRefreshPending(context, appWidgetId)
            )
            setOnClickPendingIntent(
                R.id.widget_settings,
                getConfigPending(context, appWidgetId)
            )
            setRemoteAdapter(R.id.widget_list, serviceIntent)
            setEmptyView(R.id.widget_list, R.id.empty_layout)
            appWidgetManager.updateAppWidget(appWidgetId, this)
        }
    }

    private fun getRefreshPending(
        context: Context,
        appWidgetId: Int
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

    private fun getConfigPending(context: Context, appWidgetId: Int): PendingIntent {
        val configIntent = Intent(context, ConfigActivity::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }

        return PendingIntent.getActivity(
            context, 0, configIntent, 0
        )
    }

    private fun getServiceIntent(context: Context, appWidgetId: Int, json: String): Intent {
        return Intent(context, AppWidgetService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra("json", json)
            data = Uri.parse(this.toUri(Intent.URI_INTENT_SCHEME))
        }
    }

    private fun getHeader(): JSONObject {
        return JSONObject().apply {
            put("itemTitle", "عنوان")
            put("currentPrice", "قیمت")
            put("priceChanges", "تغییر")
        }
    }

    private fun parseData(
        context: Context,
        document: Document
    ): JSONArray {
        val jsonArray = JSONArray()
        jsonArray.put(getHeader())

        getFields(context).forEach { field ->
            val elements =
                document.getElementsByAttributeValue("data-market-row", field.toString()).last()

            val jsonObject = JSONObject().apply {
                put("itemTitle", elements.child(0).text())
                put("currentPrice", elements.child(1).text())
                put("priceChanges", elements.child(2).text())
                put(
                    "changeIndicator", elements.child(2)
                        .getElementsByTag("span")
                        .attr("class")
                )
            }
            jsonArray.put(jsonObject)
        }

        return jsonArray
    }

    private fun getFields(context: Context): ArrayList<Any?> {
        val fields = ArrayList<Any?>()
        PreferenceManager.getDefaultSharedPreferences(context).all.forEach {
            fields.addAll(it.value as Collection<*>)
        }
        return fields
    }
}
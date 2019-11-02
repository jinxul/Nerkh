package com.givekesh.nerkh

import android.appwidget.AppWidgetManager
import android.content.Context
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

class DataFetcherWorker(private val context: Context, workerParams: WorkerParameters) :
    Worker(
        context,
        workerParams
    ) {

    override fun doWork(): Result {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetId = inputData.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, 17987)
        val utils = Utils(context, appWidgetId)

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
            val jsonArray = parseData(document)
            if (jsonArray.length() > 1) {
                val lastUpdate = context.resources.getString(
                    R.string.last_update,
                    SimpleDateFormat("hh:mm:ss a", Locale.US).format(Date())
                )
                utils.refreshUI(lastUpdate, jsonArray.toString(), appWidgetManager)
                return Result.success()
            }
        } catch (exception: Exception) {
            Crashlytics.logException(exception)
        }
        val json =
            context.assets.open("defaultJson.json").bufferedReader()
                .use { it.readText() }
        utils.refreshUI("", json, appWidgetManager)
        return Result.retry()
    }


    private fun getHeader(): JSONObject {
        return JSONObject().apply {
            put("itemTitle", "عنوان")
            put("currentPrice", "قیمت")
            put("priceChanges", "تغییر")
        }
    }

    private fun parseData(
        document: Document
    ): JSONArray {
        val jsonArray = JSONArray()
        jsonArray.put(getHeader())

        getFields().forEach { field ->
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

    private fun getFields(): ArrayList<Any?> {
        val fields = ArrayList<Any?>()
        PreferenceManager.getDefaultSharedPreferences(context).all.forEach {
            fields.addAll(it.value as Collection<*>)
        }
        return fields
    }
}
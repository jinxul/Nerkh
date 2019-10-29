package com.givekesh.nerkh

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.core.content.ContextCompat
import org.json.JSONArray

class AppWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val json = intent.getStringExtra("json")
        val items = JSONArray(json)
        return ListViewRemoteViewsFactory(applicationContext, items)
    }
}

class ListViewRemoteViewsFactory(private val context: Context, private val items: JSONArray) :
    RemoteViewsService.RemoteViewsFactory {

    override fun onCreate() {

    }

    override fun getLoadingView(): RemoteViews? {
        return RemoteViews(context.packageName, R.layout.loading)

    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun onDataSetChanged() {

    }

    override fun hasStableIds(): Boolean {
        return true
    }

    override fun getViewAt(position: Int): RemoteViews {
        val listItem = items.getJSONObject(position)
        val changeIndicator = listItem.optString("changeIndicator", "")
        val color = when {
            changeIndicator.contains("low") -> {
                ContextCompat.getColor(context, R.color.low)
            }
            changeIndicator.contains("high") -> {
                ContextCompat.getColor(context, R.color.high)
            }
            else -> ContextCompat.getColor(context, R.color.light)
        }
        return RemoteViews(context.packageName, R.layout.nerkh_item).apply {
            setTextViewText(R.id.title, listItem.optString("itemTitle", "---"))
            setTextViewText(R.id.price, listItem.optString("currentPrice", "---"))
            setTextViewText(R.id.changes, listItem.optString("priceChanges", "---"))
            setTextColor(R.id.price, color)
            setTextColor(R.id.changes, color)
        }
    }

    override fun getCount(): Int {
        return items.length()
    }

    override fun getViewTypeCount(): Int {
        return 1
    }

    override fun onDestroy() {
    }

}


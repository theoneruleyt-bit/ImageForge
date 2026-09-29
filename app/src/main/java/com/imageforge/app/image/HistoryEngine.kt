package com.imageforge.app.image

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class HistoryItem(
    val id: String,
    val outputUri: String,
    val createdAt: Long,
    val bytes: Long,
    val width: Int,
    val height: Int,
    val format: String,
    val source: String
)

object HistoryEngine {
    private const val PREFS = "imageforge_history"
    private const val KEY = "items"
    private const val LIMIT = 100

    fun load(context: Context): List<HistoryItem> = runCatching {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]") ?: "[]"
        val array = JSONArray(raw)
        buildList {
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                add(HistoryItem(o.getString("id"), o.getString("uri"), o.getLong("created"), o.getLong("bytes"), o.getInt("width"), o.getInt("height"), o.getString("format"), o.optString("source", "Studio")))
            }
        }.sortedByDescending { it.createdAt }
    }.getOrDefault(emptyList())

    fun add(context: Context, result: ImageProcessResult, source: String = "Studio") {
        val item = HistoryItem(UUID.randomUUID().toString(), result.outputUri.toString(), System.currentTimeMillis(), result.bytes, result.width, result.height, result.format.label, source)
        save(context, (listOf(item) + load(context)).distinctBy { it.outputUri }.take(LIMIT))
    }

    fun remove(context: Context, id: String) = save(context, load(context).filterNot { it.id == id })
    fun clear(context: Context) = save(context, emptyList())

    fun isAvailable(context: Context, item: HistoryItem): Boolean = runCatching {
        context.contentResolver.openAssetFileDescriptor(Uri.parse(item.outputUri), "r")?.use { true } ?: false
    }.getOrDefault(false)

    private fun save(context: Context, items: List<HistoryItem>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(JSONObject().apply {
                put("id", item.id); put("uri", item.outputUri); put("created", item.createdAt); put("bytes", item.bytes)
                put("width", item.width); put("height", item.height); put("format", item.format); put("source", item.source)
            })
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, array.toString()).apply()
    }
}

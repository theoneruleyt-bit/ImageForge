package com.imageforge.app.image

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class ImageRecipe(
    val id: String,
    val name: String,
    val format: OutputFormat,
    val quality: Int,
    val maxDimension: Int?,
    val removeMetadata: Boolean = false,
    val builtIn: Boolean = false
) {
    val summary: String
        get() = buildList {
            add(format.label)
            add("Q$quality")
            add(maxDimension?.let { "max $it px" } ?: "original size")
            if (removeMetadata) add("metadata off")
        }.joinToString(" • ")
}

object RecipeEngine {
    private const val PREFS = "imageforge_recipes"
    private const val KEY = "custom_recipes"

    val builtIns = listOf(
        ImageRecipe("web-ready", "Web Ready", OutputFormat.WEBP, 82, 1600, false, true),
        ImageRecipe("marketplace", "Marketplace", OutputFormat.JPEG, 90, 1600, true, true),
        ImageRecipe("private-share", "Private Share", OutputFormat.JPEG, 88, null, true, true)
    )

    fun loadCustom(context: Context): List<ImageRecipe> = runCatching {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]") ?: "[]"
        val array = JSONArray(raw)
        buildList {
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                add(ImageRecipe(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    format = OutputFormat.valueOf(o.getString("format")),
                    quality = o.getInt("quality"),
                    maxDimension = if (o.isNull("maxDimension")) null else o.getInt("maxDimension"),
                    removeMetadata = o.optBoolean("removeMetadata", false),
                    builtIn = false
                ))
            }
        }
    }.getOrDefault(emptyList())

    fun save(context: Context, recipe: ImageRecipe) {
        val current = loadCustom(context).filterNot { it.id == recipe.id } + recipe.copy(builtIn = false)
        persist(context, current)
    }

    fun delete(context: Context, id: String) {
        persist(context, loadCustom(context).filterNot { it.id == id })
    }

    private fun persist(context: Context, recipes: List<ImageRecipe>) {
        val array = JSONArray()
        recipes.forEach { r ->
            array.put(JSONObject().apply {
                put("id", r.id); put("name", r.name); put("format", r.format.name); put("quality", r.quality)
                put("maxDimension", r.maxDimension ?: JSONObject.NULL); put("removeMetadata", r.removeMetadata)
            })
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, array.toString()).apply()
    }
}

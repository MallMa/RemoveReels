package org.removereels.app

import android.content.Context
import org.json.JSONObject

/** One social platform, read from a bundled rule list (rules/<id>.json). */
class Platform(val json: JSONObject) {
    val id: String = json.getString("platform")
    val name: String = json.getString("name")
    val version: String = json.getString("version")
    val startUrl: String = json.getString("startUrl")
    val origins: Set<String> = json.getJSONArray("origins").strings().toSet()
    private val hosts: List<String> = json.getJSONArray("hosts").strings()
    val rules: List<RuleInfo> = json.getJSONArray("rules").let { array ->
        (0 until array.length()).map { RuleInfo(array.getJSONObject(it)) }
    }

    /** True for the platform's own domains and their subdomains; links anywhere else leave the app. */
    fun ownsHost(host: String?): Boolean =
        host != null && hosts.any { host == it || host.endsWith(".$it") }

    companion object {
        fun loadAll(context: Context): List<Platform> =
            context.assets.list("rules").orEmpty()
                .filter { it.endsWith(".json") }
                .sorted()
                .map { file ->
                    val text = context.assets.open("rules/$file").bufferedReader().use { it.readText() }
                    Platform(JSONObject(text))
                }
    }
}

class RuleInfo(json: JSONObject) {
    val id: String = json.getString("id")
    val name: String = json.getString("name")
    val type: String = json.getString("type")
    val default: Boolean = json.optBoolean("default", true)
    val canary: Boolean = json.optBoolean("canary", false)
}

private fun org.json.JSONArray.strings(): List<String> = (0 until length()).map { getString(it) }

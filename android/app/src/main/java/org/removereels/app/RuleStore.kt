package org.removereels.app

import android.content.Context
import org.json.JSONObject

/** Per-rule on/off switches and the on-device stats used to spot rules that stopped working. Nothing leaves the phone. */
class RuleStore(context: Context) {
    private val toggles = context.getSharedPreferences("rule_toggles", Context.MODE_PRIVATE)
    private val stats = context.getSharedPreferences("rule_stats", Context.MODE_PRIVATE)

    fun isEnabled(platform: Platform, rule: RuleInfo): Boolean =
        toggles.getBoolean(key(platform, rule.id), rule.default)

    fun setEnabled(platform: Platform, rule: RuleInfo, enabled: Boolean) {
        toggles.edit().putBoolean(key(platform, rule.id), enabled).apply()
    }

    fun enabledMap(platform: Platform): JSONObject =
        JSONObject().apply { platform.rules.forEach { put(it.id, isEnabled(platform, it)) } }

    fun stats(platform: Platform, rule: RuleInfo): RuleStats =
        RuleStats.parse(stats.getString(key(platform, rule.id), null))

    fun resetStats(platform: Platform) {
        val editor = stats.edit()
        platform.rules.forEach { editor.remove(key(platform, it.id)) }
        editor.apply()
    }

    /**
     * Records a message from the engine running in [platform]'s page. The page's own scripts can call the same bridge,
     * so the platform comes from the WebView, unknown rule ids are ignored and counts are clamped.
     */
    fun record(platform: Platform, message: String) {
        val json = runCatching { JSONObject(message) }.getOrNull() ?: return
        val known = platform.rules.associateBy { it.id }
        val editor = stats.edit()
        when (json.optString("type")) {
            "route" -> known[json.optString("id")]?.let { rule ->
                val s = stats(platform, rule)
                editor.putString(key(platform, rule.id), s.copy(hits = s.hits + 1).encode())
            }
            "pageview" -> {
                val rules = json.optJSONArray("rules") ?: return
                for (i in 0 until rules.length()) {
                    val entry = rules.optJSONObject(i) ?: continue
                    val rule = known[entry.optString("id")] ?: continue
                    val count = entry.optInt("count").coerceIn(0, 10_000)
                    val canaryChecked = rule.canary && entry.optBoolean("canary")
                    val s = stats(platform, rule)
                    editor.putString(
                        key(platform, rule.id),
                        s.copy(
                            pageViews = s.pageViews + 1,
                            hits = s.hits + count,
                            canaryChecks = s.canaryChecks + if (canaryChecked) 1 else 0,
                            canaryMisses = s.canaryMisses + if (canaryChecked && count == 0) 1 else 0,
                        ).encode()
                    )
                }
            }
        }
        editor.apply()
    }

    private fun key(platform: Platform, ruleId: String) = "${platform.id}/$ruleId"
}

data class RuleStats(val pageViews: Int = 0, val hits: Int = 0, val canaryChecks: Int = 0, val canaryMisses: Int = 0) {
    /** A canary rule that is missing on most pages where it should be visible probably no longer matches the site. */
    val looksBroken: Boolean get() = canaryChecks >= 3 && canaryMisses * 2 > canaryChecks

    fun encode() = "$pageViews,$hits,$canaryChecks,$canaryMisses"

    companion object {
        fun parse(value: String?): RuleStats {
            val parts = value?.split(',')?.mapNotNull { it.toIntOrNull() }
            return if (parts?.size == 4) RuleStats(parts[0], parts[1], parts[2], parts[3]) else RuleStats()
        }
    }
}

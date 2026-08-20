package dev.zanderp.opencfmoto

import android.content.Context
import org.json.JSONArray

data class SavedBike(val raw: String, val name: String = "800NK Advanced") {
    val qr: QrData? get() = QrData.parse(raw)
}

/** Stores the single CFMOTO 800NK Advanced pairing QR used by the app. */
object BikeMemory {
    private const val PREFS = "opencfmoto_bike"
    private const val KEY_RAW = "last_qr_raw"
    private const val KEY_NAME = "last_bike_name"
    private const val LEGACY_LIST = "bikes_json"
    private const val LEGACY_SELECTED = "selected_raw"

    private fun prefs(ctx: Context) =
        ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun save(ctx: Context, raw: String, qr: QrData) {
        prefs(ctx).edit()
            .putString(KEY_RAW, raw)
            .putString(KEY_NAME, "800NK Advanced")
            .remove(LEGACY_LIST)
            .remove(LEGACY_SELECTED)
            .apply()
    }

    fun selected(ctx: Context): SavedBike? {
        migrateLegacy(ctx)
        val raw = prefs(ctx).getString(KEY_RAW, null)?.takeIf { it.isNotBlank() } ?: return null
        return SavedBike(raw)
    }

    fun clear(ctx: Context) {
        prefs(ctx).edit()
            .remove(KEY_RAW)
            .remove(KEY_NAME)
            .remove(LEGACY_LIST)
            .remove(LEGACY_SELECTED)
            .apply()
    }

    fun lastRaw(ctx: Context): String? = selected(ctx)?.raw
    fun lastQr(ctx: Context): QrData? = selected(ctx)?.qr
    fun lastBikeName(ctx: Context): String? = selected(ctx)?.name
    fun hasSaved(ctx: Context): Boolean = selected(ctx) != null

    /** Import the previous pairing once, then discard the legacy storage. */
    private fun migrateLegacy(ctx: Context) {
        val p = prefs(ctx)
        if (!p.getString(KEY_RAW, null).isNullOrBlank()) return
        val list = runCatching { JSONArray(p.getString(LEGACY_LIST, "[]")) }.getOrNull() ?: return
        val selectedRaw = p.getString(LEGACY_SELECTED, null)
        var raw: String? = null
        for (i in 0 until list.length()) {
            val candidate = list.optJSONObject(i)?.optString("raw")?.takeIf { it.isNotBlank() } ?: continue
            if (raw == null) raw = candidate
            if (candidate == selectedRaw) {
                raw = candidate
                break
            }
        }
        if (raw != null) {
            p.edit()
                .putString(KEY_RAW, raw)
                .putString(KEY_NAME, "800NK Advanced")
                .remove(LEGACY_LIST)
                .remove(LEGACY_SELECTED)
                .apply()
        }
    }
}

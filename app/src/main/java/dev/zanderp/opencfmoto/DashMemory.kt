// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Alexandru <https://alexandru.rocks> and the OpenCfMoto contributors.
// Part of OpenCfMoto. Free software under the GNU AGPL v3 or later; see LICENSE and NOTICE.
package dev.zanderp.opencfmoto

import android.content.Context

/**
 * Remembers the canvas size each dash asks for (`REQ_CONFIG_CAPTURE` width×height), keyed by SSID.
 *
 * The measurement is used only to refine match-aspect margins for the fixed portrait stream.
 */
object DashMemory {
    private const val PREFS = "opencfmoto_bike"
    private const val KEY_PREFIX = "dash_geo_"
    private const val KEY_LAST_TOUCH = "last_dash_touch"

    private fun prefs(ctx: Context) =
        ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun setLastDashTouch(ctx: Context, touch: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_LAST_TOUCH, touch).apply()
    }

    fun lastDashTouch(ctx: Context): Boolean? {
        val p = prefs(ctx)
        return if (p.contains(KEY_LAST_TOUCH)) p.getBoolean(KEY_LAST_TOUCH, false) else null
    }

    fun observe(ctx: Context, ssid: String?, w: Int, h: Int) {
        if (ssid.isNullOrBlank() || w <= 0 || h <= 0) return
        val key = KEY_PREFIX + ssid
        val now = "${w}x$h"
        val prior = prefs(ctx).getString(key, null)
        prefs(ctx).edit().putString(key, now).apply()
        if (prior != now) {
            LogBus.log(
                "[panel] learned 800NK canvas $now — next connect will refine match-aspect margins",
            )
        }
    }

    fun get(ctx: Context, ssid: String?): Pair<Int, Int>? {
        if (ssid.isNullOrBlank()) return null
        val v = prefs(ctx).getString(KEY_PREFIX + ssid, null) ?: return null
        val parts = v.split("x")
        val w = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val h = parts.getOrNull(1)?.toIntOrNull() ?: return null
        return if (w in 1..8192 && h in 1..8192) w to h else null
    }

}

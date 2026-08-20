// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Alexandru <https://alexandru.rocks> and the OpenCfMoto contributors.
// Part of OpenCfMoto. Free software under the GNU AGPL v3 or later; see LICENSE and NOTICE.
// Handlebar-button control model ported from the ionutradu252/open-cfmoto fork.
package dev.zanderp.opencfmoto

import android.content.Context

/**
 * What the bike's Bluetooth media buttons (track/play-pause) should do:
 *   true            = control ANDROID AUTO UI — [MediaButtonBridge] keeps exclusive AVRCP ownership
 *                     and remaps keys to navigation. Music apps must not get the bars; control
 *                     playback by navigating the AA UI with those same buttons.
 *   false (default) = control MEDIA — buttons retain normal media, volume and call behavior.
 *
 * Persisted so the choice survives restarts.
 */
object ButtonMode {
    private const val PREF = "button_mode"
    private const val KEY = "controlAa"
    internal const val DEFAULT_CONTROL_AA = false

    internal fun shouldCapture(controlAa: Boolean, voiceActive: Boolean): Boolean =
        controlAa && !voiceActive

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun isControlAa(context: Context): Boolean =
        BikeScope.getBoolean(prefs(context), context, KEY, DEFAULT_CONTROL_AA)

    fun set(context: Context, controlAa: Boolean) {
        BikeScope.putBoolean(prefs(context), context, KEY, controlAa)
    }
}

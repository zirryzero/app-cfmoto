// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Alexandru <https://alexandru.rocks> and the OpenCfMoto contributors.
// Part of OpenCfMoto. Free software under the GNU AGPL v3 or later; see LICENSE and NOTICE.
package dev.zanderp.opencfmoto

import android.content.Context

/**
 * Small on/off app preferences (kept apart from the video-tuning [VideoPrefs]).
 *
 *  - [autoConnect]  — on launch, if a bike is paired and its Wi-Fi is in range, start connecting
 *    automatically instead of waiting for a tap. Default on; the rider can turn it off.
 *  - [autoRecovery] — the watchdog ([AndroidAutoService]) restarts the bike link if projection
 *    stalls or the dash drops, without a manual Stop/Start. Default on.
 * The dashboard profile, touchscreen capability and Wi-Fi transport are fixed for the 800NK
 * Advanced and are therefore not user preferences.
 */
object AppSettings {
    private const val PREFS = "opencfmoto_bike"
    private const val KEY_AUTO_CONNECT = "auto_connect"
    private const val KEY_AUTO_RECOVERY = "auto_recovery"
    private const val KEY_LOG_TRIPS = "log_trips"
    private const val KEY_INCLUDE_SECRETS = "include_secrets_in_logs"
    private const val KEY_ANON_TELEMETRY = "anonymous_telemetry"
    private const val KEY_BT_CLOCK = "bluetooth_clock_sync"
    private const val KEY_KEEP_WIFI = "keep_wifi_after_disconnect"

    private fun prefs(ctx: Context) =
        ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun autoConnect(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_AUTO_CONNECT, true)
    fun setAutoConnect(ctx: Context, on: Boolean) =
        prefs(ctx).edit().putBoolean(KEY_AUTO_CONNECT, on).apply()

    fun autoRecovery(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_AUTO_RECOVERY, true)
    fun setAutoRecovery(ctx: Context, on: Boolean) =
        prefs(ctx).edit().putBoolean(KEY_AUTO_RECOVERY, on).apply()

    fun logTrips(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_LOG_TRIPS, true)
    fun setLogTrips(ctx: Context, on: Boolean) =
        prefs(ctx).edit().putBoolean(KEY_LOG_TRIPS, on).apply()

    /** When true, Share Logs / LogBus keep Wi‑Fi passwords and serials. Default off. */
    fun includeSecretsInLogs(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_INCLUDE_SECRETS, false)
    fun setIncludeSecretsInLogs(ctx: Context, on: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_INCLUDE_SECRETS, on).apply()
        LogBus.includeSecrets = on
    }

    /**
     * Anonymous install ping + crash/error upload (random UUID only). Default **on**;
     * rider can turn off in Setup → Privacy. See PRIVACY.md.
     */
    /** Answer dash clock over BLE (EC-BTP). Off by default — bike must already be paired. */
    fun bluetoothClockSync(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_BT_CLOCK, false)
    fun setBluetoothClockSync(ctx: Context, on: Boolean) =
        prefs(ctx).edit().putBoolean(KEY_BT_CLOCK, on).apply()

    /**
     * Stay associated to the 800NK SoftAP after Stop so the dashboard can retain clock state.
     * Process is unbound so cellular/maps still work. Off by default.
     */
    fun keepWifiAfterDisconnect(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_KEEP_WIFI, false)
    fun setKeepWifiAfterDisconnect(ctx: Context, on: Boolean) =
        prefs(ctx).edit().putBoolean(KEY_KEEP_WIFI, on).apply()

    fun anonymousTelemetry(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_ANON_TELEMETRY, true)
    fun setAnonymousTelemetry(ctx: Context, on: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_ANON_TELEMETRY, on).apply()
        if (!on) AnonymousTelemetry.onDisabled(ctx)
    }

    /** Sync holder flags from prefs (call on process start / before connect). */
    fun applyToHolder(ctx: Context) {
        BikeProfileHolder.active = BikeProfiles.only
        LogBus.includeSecrets = includeSecretsInLogs(ctx)
        ButtonMap.ensureDefaultsMigrated(ctx)
        ScreenMargins.load(ctx)
    }
}

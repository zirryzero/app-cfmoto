package dev.zanderp.opencfmoto

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.view.accessibility.AccessibilityManager

/** Process-wide state for the app mirroring and remote-touch mode. */
object AppModeController {
    @Volatile private var active = false
    @Volatile private var selectedPackage: String? = null
    @Volatile private var selectedLabel: String? = null
    @Volatile private var captureWidth = 0
    @Volatile private var captureHeight = 0

    val isActive: Boolean get() = active
    val appPackage: String? get() = selectedPackage
    val appLabel: String? get() = selectedLabel

    fun activate(context: Context, packageName: String, label: String) {
        selectedPackage = packageName
        selectedLabel = label
        active = true
        AppControlAccessibilityService.instance?.setAppControlsVisible(true)
        LogBus.log("[APPS] mode active for $label ($packageName); touch=${isAccessibilityEnabled(context)}")
    }

    fun select(packageName: String, label: String) {
        selectedPackage = packageName
        selectedLabel = label
        LogBus.log("[APPS] selected $label ($packageName)")
    }

    fun deactivate() {
        if (active) LogBus.log("[APPS] mode stopped")
        active = false
        selectedPackage = null
        selectedLabel = null
        captureWidth = 0
        captureHeight = 0
        AppControlAccessibilityService.instance?.setAppControlsVisible(false)
    }

    fun updateCaptureSize(width: Int, height: Int) {
        if (!active) return
        captureWidth = width.coerceAtLeast(1)
        captureHeight = height.coerceAtLeast(1)
    }

    fun dispatchBikeTouch(action: Int, pointerId: Int, sourceX: Int, sourceY: Int): Boolean {
        if (!active) return false
        val service = AppControlAccessibilityService.instance ?: return false
        val w = captureWidth
        val h = captureHeight
        if (w <= 0 || h <= 0) return false
        service.acceptRemoteTouch(action, pointerId, sourceX, sourceY, w, h)
        return true
    }

    fun launchSelected(context: Context): Boolean {
        val packageName = selectedPackage ?: return false
        return try {
            val launch = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            context.startActivity(launch)
            LogBus.log("[APPS] launched ${selectedLabel ?: packageName}")
            true
        } catch (e: Exception) {
            LogBus.log("[APPS] launch failed for $packageName: $e")
            false
        }
    }

    fun openSwitcher(context: Context) {
        try {
            context.startActivity(AppLauncherActivity.createIntent(context, switchOnly = true))
        } catch (e: Exception) {
            LogBus.log("[APPS] app switcher failed: $e")
        }
    }

    fun isAccessibilityEnabled(context: Context): Boolean {
        if (AppControlAccessibilityService.instance != null) return true
        return try {
            val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
            manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK).any { info ->
                val service = info.resolveInfo.serviceInfo
                service.packageName == context.packageName &&
                    service.name == AppControlAccessibilityService::class.java.name
            }
        } catch (_: Exception) {
            false
        }
    }
}

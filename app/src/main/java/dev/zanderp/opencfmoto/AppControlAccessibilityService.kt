package dev.zanderp.opencfmoto

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Color
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.appcompat.content.res.AppCompatResources
import java.lang.ref.WeakReference
import java.util.ArrayDeque

/** Injects dashboard gestures into the foreground phone app and provides app-mode navigation. */
class AppControlAccessibilityService : AccessibilityService() {
    companion object {
        @Volatile private var instanceRef = WeakReference<AppControlAccessibilityService>(null)
        val instance: AppControlAccessibilityService? get() = instanceRef.get()
    }

    private data class Point(val x: Float, val y: Float)
    private data class Trace(
        val pointerId: Int,
        val startedAt: Long,
        var endedAt: Long,
        val points: MutableList<Point> = ArrayList(),
    )

    private val main = Handler(Looper.getMainLooper())
    private val traces = LinkedHashMap<Int, Trace>()
    private val completed = ArrayList<Trace>()
    private val gestureQueue = ArrayDeque<List<Trace>>()
    private var gestureInFlight = false
    private var controls: View? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instanceRef = WeakReference(this)
        LogBus.log("[APPS] accessibility touch control connected")
        setAppControlsVisible(AppModeController.isActive)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() {
        synchronized(traces) {
            traces.clear()
            completed.clear()
            gestureQueue.clear()
        }
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        if (instance === this) instanceRef.clear()
        removeControls()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        if (instance === this) instanceRef.clear()
        removeControls()
        super.onDestroy()
    }

    fun acceptRemoteTouch(
        action: Int,
        pointerId: Int,
        sourceX: Int,
        sourceY: Int,
        sourceWidth: Int,
        sourceHeight: Int,
    ) {
        val bounds = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                getSystemService(WindowManager::class.java).currentWindowMetrics.bounds
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }
        val targetWidth = bounds?.width() ?: resources.displayMetrics.widthPixels
        val targetHeight = bounds?.height() ?: resources.displayMetrics.heightPixels
        val point = AppTouchGeometry.scalePoint(
            sourceX,
            sourceY,
            sourceWidth,
            sourceHeight,
            targetWidth,
            targetHeight,
        ) ?: return
        val now = android.os.SystemClock.uptimeMillis()

        synchronized(traces) {
            when (action) {
                0 -> {
                    if (traces.isEmpty()) completed.clear()
                    traces[pointerId] = Trace(pointerId, now, now).also {
                        it.points += Point(point.first, point.second)
                    }
                }
                2 -> traces[pointerId]?.let { trace ->
                    trace.endedAt = now
                    val last = trace.points.lastOrNull()
                    if (last == null || kotlin.math.abs(last.x - point.first) >= 1f ||
                        kotlin.math.abs(last.y - point.second) >= 1f
                    ) {
                        if (trace.points.size < 180) trace.points += Point(point.first, point.second)
                    }
                }
                1 -> {
                    val trace = traces.remove(pointerId) ?: return
                    trace.endedAt = now
                    trace.points += Point(point.first, point.second)
                    completed += trace
                    if (traces.isEmpty()) {
                        gestureQueue.addLast(completed.map { it.copy(points = ArrayList(it.points)) })
                        completed.clear()
                        dispatchNextLocked()
                    }
                }
            }
        }
    }

    private fun dispatchNextLocked() {
        if (gestureInFlight || gestureQueue.isEmpty()) return
        val batch = gestureQueue.removeFirst()
        gestureInFlight = true
        main.post { dispatchBatch(batch) }
    }

    private fun dispatchBatch(batch: List<Trace>) {
        if (batch.isEmpty()) {
            gestureFinished()
            return
        }
        val firstStart = batch.minOf { it.startedAt }
        val builder = GestureDescription.Builder()
        for (trace in batch.take(2)) {
            val path = Path()
            val first = trace.points.firstOrNull() ?: continue
            path.moveTo(first.x, first.y)
            for (point in trace.points.drop(1)) path.lineTo(point.x, point.y)
            val startOffset = (trace.startedAt - firstStart).coerceIn(0L, 5_000L)
            val duration = (trace.endedAt - trace.startedAt).coerceIn(40L, 5_000L)
            builder.addStroke(GestureDescription.StrokeDescription(path, startOffset, duration))
        }
        val gesture = try {
            builder.build()
        } catch (e: Exception) {
            LogBus.log("[APPS] invalid remote gesture: $e")
            gestureFinished()
            return
        }
        val accepted = dispatchGesture(
            gesture,
            object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) = gestureFinished()
                override fun onCancelled(gestureDescription: GestureDescription?) {
                    LogBus.log("[APPS] remote gesture cancelled")
                    gestureFinished()
                }
            },
            main,
        )
        if (!accepted) {
            LogBus.log("[APPS] Android rejected remote gesture")
            gestureFinished()
        }
    }

    private fun gestureFinished() {
        synchronized(traces) {
            gestureInFlight = false
            dispatchNextLocked()
        }
    }

    fun setAppControlsVisible(visible: Boolean) {
        main.post {
            if (visible) showControls() else removeControls()
        }
    }

    private fun showControls() {
        if (controls != null) return
        val density = resources.displayMetrics.density
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            val pad = (4 * density).toInt()
            setPadding(pad, pad, pad, pad)
            background = GradientDrawable().apply {
                setColor(Color.argb(232, 20, 27, 32))
                cornerRadius = 8 * density
                setStroke((1 * density).toInt().coerceAtLeast(1), Color.rgb(48, 65, 74))
            }
        }

        fun button(icon: Int, description: Int, action: () -> Unit): ImageButton =
            ImageButton(this).apply {
                setImageResource(icon)
                contentDescription = getString(description)
                setColorFilter(Color.WHITE)
                background = AppCompatResources.getDrawable(
                    this@AppControlAccessibilityService,
                    android.R.drawable.list_selector_background,
                )
                scaleType = android.widget.ImageView.ScaleType.CENTER_INSIDE
                setPadding((11 * density).toInt(), (11 * density).toInt(), (11 * density).toInt(), (11 * density).toInt())
                setOnClickListener { action() }
                layoutParams = LinearLayout.LayoutParams((46 * density).toInt(), (46 * density).toInt())
            }

        bar.addView(button(R.drawable.ic_back, R.string.apps_back) {
            performGlobalAction(GLOBAL_ACTION_BACK)
        })
        bar.addView(button(R.drawable.ic_home, R.string.apps_home) {
            performGlobalAction(GLOBAL_ACTION_HOME)
        })
        bar.addView(button(R.drawable.ic_recent, R.string.apps_recents) {
            performGlobalAction(GLOBAL_ACTION_RECENTS)
        })
        bar.addView(button(R.drawable.ic_apps, R.string.apps_switch) {
            AppModeController.openSwitcher(this)
        })

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = (24 * density).toInt()
        }
        try {
            getSystemService(WindowManager::class.java).addView(bar, params)
            controls = bar
        } catch (e: Exception) {
            LogBus.log("[APPS] navigation bar failed: $e")
        }
    }

    private fun removeControls() {
        val view = controls ?: return
        controls = null
        try {
            getSystemService(WindowManager::class.java).removeView(view)
        } catch (_: Exception) {
        }
    }
}

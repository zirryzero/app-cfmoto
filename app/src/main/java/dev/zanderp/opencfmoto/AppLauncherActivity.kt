package dev.zanderp.opencfmoto

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.GridView
import android.widget.ImageView
import android.widget.TextView
import android.widget.ProgressBar
import android.widget.Toast
import kotlin.concurrent.thread
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.button.MaterialButton
import java.util.Locale

/** Selects a launchable phone app for full-screen capture and dashboard touch control. */
class AppLauncherActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_PACKAGE = "app_package"
        const val EXTRA_LABEL = "app_label"
        const val EXTRA_MIRROR = "mirror_screen"
        private const val EXTRA_SWITCH_ONLY = "switch_only"

        fun createIntent(context: Context, switchOnly: Boolean = false): Intent =
            Intent(context, AppLauncherActivity::class.java).apply {
                putExtra(EXTRA_SWITCH_ONLY, switchOnly)
                if (switchOnly) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
    }

    private data class Entry(
        val packageName: String?,
        val label: String,
        val icon: Drawable,
        val mirror: Boolean = false,
    )

    private var adapter: AppAdapter? = null
    private lateinit var accessibilityStatus: TextView
    private lateinit var accessibilityButton: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var gridView: GridView
    private val switchOnly: Boolean get() = intent.getBooleanExtra(EXTRA_SWITCH_ONLY, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_launcher)

        accessibilityStatus = findViewById(R.id.apps_accessibility_status)
        accessibilityButton = findViewById(R.id.apps_accessibility_button)
        accessibilityButton.setOnClickListener { openAccessibilitySettings() }
        findViewById<View>(R.id.apps_close).setOnClickListener { finish() }

        progressBar = findViewById(R.id.apps_progress)
        gridView = findViewById(R.id.apps_grid)

        val searchInput = findViewById<EditText>(R.id.apps_search)
        searchInput.doAfterTextChanged { text ->
            adapter?.filter(text?.toString().orEmpty())
        }

        thread(name = "load-apps", isDaemon = true) {
            val entries = loadApps()
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                adapter = AppAdapter(entries).also {
                    gridView.adapter = it
                }
                gridView.setOnItemClickListener { _, _, position, _ ->
                    adapter?.item(position)?.let { choose(it) }
                }
                val currentQuery = searchInput.text?.toString().orEmpty()
                if (currentQuery.isNotEmpty()) {
                    adapter?.filter(currentQuery)
                }
                progressBar.visibility = View.GONE
                gridView.visibility = View.VISIBLE
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshAccessibilityState()
    }

    private fun loadApps(): List<Entry> {
        val result = ArrayList<Entry>()
        if (!switchOnly) {
            result += Entry(
                packageName = null,
                label = getString(R.string.apps_entire_screen),
                icon = requireNotNull(AppCompatResources.getDrawable(this, R.drawable.ic_cast)),
                mirror = true,
            )
        }
        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val launchers: List<ResolveInfo> = packageManager.queryIntentActivities(query, 0)
        val seen = HashSet<String>()
        launchers
            .sortedBy { it.loadLabel(packageManager).toString().lowercase(Locale.getDefault()) }
            .forEach { info ->
                val packageName = info.activityInfo.packageName
                if (packageName == this.packageName || !seen.add(packageName)) return@forEach
                result += Entry(
                    packageName = packageName,
                    label = info.loadLabel(packageManager).toString(),
                    icon = info.loadIcon(packageManager),
                )
            }
        return result
    }

    private fun choose(entry: Entry) {
        if (entry.mirror) {
            setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_MIRROR, true))
            finish()
            return
        }
        val packageName = entry.packageName ?: return
        if (switchOnly) {
            AppModeController.select(packageName, entry.label)
            if (!AppModeController.launchSelected(this)) {
                Toast.makeText(this, R.string.apps_launch_failed, Toast.LENGTH_SHORT).show()
                return
            }
            finish()
            return
        }
        setResult(
            Activity.RESULT_OK,
            Intent()
                .putExtra(EXTRA_PACKAGE, packageName)
                .putExtra(EXTRA_LABEL, entry.label),
        )
        finish()
    }

    private fun refreshAccessibilityState() {
        val enabled = AppModeController.isAccessibilityEnabled(this)
        accessibilityStatus.setText(
            if (enabled) R.string.apps_touch_enabled else R.string.apps_touch_disabled,
        )
        accessibilityButton.setText(
            if (enabled) R.string.apps_touch_settings else R.string.apps_enable_touch,
        )
    }

    private fun openAccessibilitySettings() {
        try {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        } catch (_: Exception) {
            Toast.makeText(this, R.string.apps_settings_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private inner class AppAdapter(private val all: List<Entry>) : BaseAdapter() {
        private val visible = ArrayList(all)

        fun item(position: Int): Entry = visible[position]

        fun filter(query: String) {
            val needle = query.trim().lowercase(Locale.getDefault())
            visible.clear()
            visible += if (needle.isBlank()) all else all.filter {
                it.label.lowercase(Locale.getDefault()).contains(needle)
            }
            notifyDataSetChanged()
        }

        override fun getCount(): Int = visible.size
        override fun getItem(position: Int): Entry = visible[position]
        override fun getItemId(position: Int): Long = visible[position].packageName?.hashCode()?.toLong() ?: -1L

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(parent.context)
                .inflate(R.layout.row_app_tile, parent, false)
            val entry = visible[position]
            view.findViewById<ImageView>(R.id.app_icon).setImageDrawable(entry.icon)
            view.findViewById<TextView>(R.id.app_label).text = entry.label
            return view
        }
    }
}

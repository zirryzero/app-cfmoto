package dev.zanderp.opencfmoto

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

internal data class LanguageOption(val tag: String, val nativeName: String)

/** Supported in-app languages. An empty tag delegates language selection to Android. */
internal object AppLanguage {
    val supported = listOf(
        LanguageOption("en", "English"),
        LanguageOption("de", "Deutsch"),
        LanguageOption("it", "Italiano"),
        LanguageOption("fr", "Français"),
        LanguageOption("es", "Español"),
        LanguageOption("ca", "Català"),
        LanguageOption("pt", "Português"),
        LanguageOption("pl", "Polski"),
        LanguageOption("cs", "Čeština"),
        LanguageOption("ro", "Română"),
        LanguageOption("nl", "Nederlands"),
        LanguageOption("hu", "Magyar"),
        LanguageOption("tr", "Türkçe"),
        LanguageOption("ko", "한국어"),
    )

    fun options(context: Context): List<LanguageOption> =
        listOf(LanguageOption("", context.getString(R.string.setup_language_system))) + supported

    fun selectedTag(): String =
        AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore(',')

    fun selectedLabel(context: Context): String {
        val tag = selectedTag()
        if (tag.isBlank()) return context.getString(R.string.setup_language_system)
        return supported.firstOrNull { it.tag.equals(tag, ignoreCase = true) }?.nativeName ?: tag
    }

    fun select(tag: String) {
        val locales = if (tag.isBlank()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(tag)
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }
}

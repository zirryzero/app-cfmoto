package dev.zanderp.opencfmoto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLanguageTest {

    @Test
    fun supportedLanguagesMatchLocaleConfig() {
        assertEquals(
            listOf("en", "de", "it", "fr", "es", "ca", "pt", "pl", "cs", "ro", "nl", "hu", "tr", "ko"),
            AppLanguage.supported.map { it.tag },
        )
    }

    @Test
    fun languageTagsAndNamesAreUniqueAndNonBlank() {
        val tags = AppLanguage.supported.map { it.tag }
        val names = AppLanguage.supported.map { it.nativeName }

        assertEquals(tags.size, tags.distinct().size)
        assertEquals(names.size, names.distinct().size)
        assertTrue(tags.all { it.isNotBlank() })
        assertTrue(names.all { it.isNotBlank() })
    }
}

package se.vadretapp.vadret.ui.format

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LanguageTagTest {

    @Test
    fun `the current language tag uses dashes and names a language`() {
        val languageTag: String = currentLanguageTag()

        assertFalse(actual = languageTag.contains(char = '_'))
        assertTrue(actual = languageTag.isNotBlank())
    }
}
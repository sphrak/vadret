package se.vadretapp.vadret.di

import android.content.Context
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals

class FdroidModulesTest {
    @Test
    fun `this build has no push alerts and never talks to the push server`() {
        val context: Context = mockk(relaxed = true)

        assertEquals(
            expected = emptyList(),
            actual = flavorModules(context = context)
        )
    }
}
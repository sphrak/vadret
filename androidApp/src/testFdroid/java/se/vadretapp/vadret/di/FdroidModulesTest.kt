package se.vadretapp.vadret.di

import android.content.Context
import io.github.sphrak.either.extension.asRight
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.koin.dsl.koinApplication
import se.vadretapp.vadret.aggregatedfeed.model.ForecastWarningRequest
import se.vadretapp.vadret.warning.usecase.GetForecastWarningRequestTask
import kotlin.test.Test
import kotlin.test.assertEquals

class FdroidModulesTest {
    @Test
    fun `this build has no push alerts and never talks to the push server`() =
        runTest {
            val context: Context = mockk(relaxed = true)
            val flavorModules = flavorModules(context = context)
            val getForecastWarningRequestTask: GetForecastWarningRequestTask = koinApplication { modules(modules = flavorModules) }.koin.get()

            assertEquals(
                expected = 1,
                actual = flavorModules.size
            )
            assertEquals(
                expected = null.asRight<ForecastWarningRequest?>(),
                actual = getForecastWarningRequestTask()
            )
        }
}